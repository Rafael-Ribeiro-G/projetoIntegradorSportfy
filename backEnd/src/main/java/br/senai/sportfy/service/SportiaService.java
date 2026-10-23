package br.senai.sportfy.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.senai.sportfy.exception.SportiaException;

/**
 * Ponte para o SportIA (serviço Python do Gui, só em 127.0.0.1:8001 no servidor).
 * O app fala só com a API; a chave da Groq fica no serviço Python e nunca sai do servidor.
 * Cada usuário pode mandar até {@value #LIMITE_PERGUNTAS} perguntas a cada 10 minutos (protege a cota da Groq).
 */
@Service
public class SportiaService {

    private static final Logger log = LoggerFactory.getLogger(SportiaService.class);

    public static final int LIMITE_PERGUNTAS = 20;
    private static final Duration JANELA_LIMITE = Duration.ofMinutes(10);
    /** Campos aceitos na resposta do Python, na ordem: o pacote V11.0 usa "resposta". */
    private static final java.util.List<String> CAMPOS_RESPOSTA =
            java.util.List.of("resposta", "response", "reply", "answer", "message", "content", "text");

    private final ObjectMapper json;
    private final Clock clock;
    private final URI url;
    private final Duration timeout;
    private final HttpClient cliente = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final Map<UUID, Deque<Instant>> perguntasPorUsuario = new ConcurrentHashMap<>();

    public SportiaService(ObjectMapper json, Clock clock,
                          @Value("${SPORTIA_URL:http://127.0.0.1:8001/chat}") String url,
                          @Value("${SPORTIA_TIMEOUT_SEGUNDOS:40}") long timeoutSegundos) {
        this.json = json;
        this.clock = clock;
        this.url = URI.create(url);
        this.timeout = Duration.ofSeconds(timeoutSegundos);
    }

    public String perguntar(UUID usuarioId, String pergunta) {
        registrarPergunta(usuarioId);
        try {
            // "pergunta" (pacote V11.0) e "message" (versão do Gui no servidor): o Python usa o que conhece
            String texto = pergunta.strip();
            String corpo = json.writeValueAsString(Map.of("pergunta", texto, "message", texto));
            HttpRequest requisicao = HttpRequest.newBuilder(url)
                    .header("Content-Type", "application/json; charset=utf-8")
                    .timeout(timeout)
                    .POST(HttpRequest.BodyPublishers.ofString(corpo))
                    .build();
            HttpResponse<String> retorno = cliente.send(requisicao, HttpResponse.BodyHandlers.ofString());
            if (retorno.statusCode() != 200) {
                log.warn("SportIA respondeu HTTP {}", retorno.statusCode());
                throw new SportiaException(HttpStatus.SERVICE_UNAVAILABLE,
                        "O SportIA está indisponível. Tente novamente em instantes.");
            }
            JsonNode corpoResposta = json.readTree(retorno.body());
            for (String campo : CAMPOS_RESPOSTA) {
                JsonNode resposta = corpoResposta.path(campo);
                if (resposta.isTextual() && !resposta.asText().isBlank()) {
                    return resposta.asText().strip();
                }
            }
            log.warn("SportIA respondeu 200 sem texto de resposta (campos: {})", nomesDosCampos(corpoResposta));
            throw new SportiaException(HttpStatus.BAD_GATEWAY, "O SportIA não conseguiu responder. Tente de novo.");
        } catch (HttpTimeoutException ex) {
            throw new SportiaException(HttpStatus.GATEWAY_TIMEOUT, "O SportIA demorou demais para responder.");
        } catch (IOException ex) {
            log.warn("Falha ao conectar com o SportIA em {}: {}", url, ex.toString());
            throw new SportiaException(HttpStatus.SERVICE_UNAVAILABLE,
                    "O SportIA está indisponível. Tente novamente em instantes.");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new SportiaException(HttpStatus.SERVICE_UNAVAILABLE, "A consulta ao SportIA foi interrompida.");
        }
    }

    private static String nomesDosCampos(JsonNode no) {
        java.util.List<String> nomes = new java.util.ArrayList<>();
        no.fieldNames().forEachRemaining(nomes::add);
        return String.join(", ", nomes);
    }

    private void registrarPergunta(UUID usuarioId) {
        Instant agora = clock.instant();
        Deque<Instant> fila = perguntasPorUsuario.computeIfAbsent(usuarioId, id -> new ArrayDeque<>());
        synchronized (fila) {
            while (!fila.isEmpty() && fila.peekFirst().isBefore(agora.minus(JANELA_LIMITE))) {
                fila.pollFirst();
            }
            if (fila.size() >= LIMITE_PERGUNTAS) {
                throw new SportiaException(HttpStatus.TOO_MANY_REQUESTS,
                        "Você fez muitas perguntas seguidas. Aguarde alguns minutos.");
            }
            fila.addLast(agora);
        }
    }
}
