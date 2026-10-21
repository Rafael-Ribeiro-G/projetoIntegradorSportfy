package br.senai.sportfy.integracao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpServer;

import br.senai.sportfy.service.SportiaService;

/** Chatbot: a API repassa a pergunta a um SportIA falso (servidor HTTP local no lugar do Python). */
class SportiaIntegracaoTest extends IntegracaoBase {

    private static final HttpServer SPORTIA_FALSO;
    private static final AtomicReference<String> ULTIMO_CORPO = new AtomicReference<>();

    static {
        try {
            SPORTIA_FALSO = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        SPORTIA_FALSO.createContext("/chat", troca -> {
            String corpo = new String(troca.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            ULTIMO_CORPO.set(corpo);
            int status = 200;
            String resposta = "{\"resposta\":\"Toque em Participar na tela do evento.\"}";
            if (corpo.contains("quebrar")) {
                status = 503;
                resposta = "{\"detail\":\"SportIA indisponível no momento\"}";
            } else if (corpo.contains("vazio")) {
                resposta = "{\"resposta\":\"  \"}";
            }
            byte[] bytes = resposta.getBytes(StandardCharsets.UTF_8);
            troca.getResponseHeaders().add("Content-Type", "application/json");
            troca.sendResponseHeaders(status, bytes.length);
            try (OutputStream out = troca.getResponseBody()) {
                out.write(bytes);
            }
        });
        SPORTIA_FALSO.start();
    }

    @DynamicPropertySource
    static void apontarParaSportiaFalso(DynamicPropertyRegistry registry) {
        registry.add("SPORTIA_URL", () -> "http://127.0.0.1:" + SPORTIA_FALSO.getAddress().getPort() + "/chat");
        // esta classe sobe um contexto próprio: usa um banco H2 separado para não recriar as tabelas do outro
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:sportfy-chat;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;"
                + "DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1");
    }

    @AfterAll
    static void desligar() {
        SPORTIA_FALSO.stop(0);
    }

    private ObjectNode pergunta(String texto) {
        ObjectNode body = json.createObjectNode();
        body.put("pergunta", texto);
        return body;
    }

    @Test
    void perguntaDeUsuarioLogado_devolveRespostaDoSportia() throws Exception {
        String token = tokenDe(registrar("Curioso"));
        JsonNode r = enviar(post("/api/chatbot/mensagem"), token, pergunta("  Como participar de um evento?  "), 200);

        assertThat(r.get("resposta").asText()).isEqualTo("Toque em Participar na tela do evento.");
        // só a pergunta (sem token nem dados do usuário) vai para o serviço Python
        assertThat(json.readTree(ULTIMO_CORPO.get()).toString())
                .isEqualTo("{\"pergunta\":\"Como participar de um evento?\"}");
    }

    @Test
    void semLogin_401_ePerguntaVaziaOuGrande_400() throws Exception {
        enviar(post("/api/chatbot/mensagem"), null, pergunta("Oi"), 401);
        String token = tokenDe(registrar("Apressado"));
        enviar(post("/api/chatbot/mensagem"), token, pergunta("   "), 400);
        enviar(post("/api/chatbot/mensagem"), token, pergunta("a".repeat(1001)), 400);
    }

    @Test
    void sportiaComErro_503_eRespostaVazia_502() throws Exception {
        String token = tokenDe(registrar("Azarado"));
        JsonNode erro = enviar(post("/api/chatbot/mensagem"), token, pergunta("quebrar"), 503);
        assertThat(erro.get("mensagem").asText()).contains("indisponível");
        enviar(post("/api/chatbot/mensagem"), token, pergunta("vazio"), 502);
    }

    @Test
    void muitasPerguntasSeguidas_429() throws Exception {
        String token = tokenDe(registrar("Tagarela"));
        for (int i = 0; i < SportiaService.LIMITE_PERGUNTAS; i++) {
            enviar(post("/api/chatbot/mensagem"), token, pergunta("Pergunta " + i), 200);
        }
        enviar(post("/api/chatbot/mensagem"), token, pergunta("Mais uma"), 429);
        // o limite é por usuário: outra pessoa continua perguntando
        enviar(post("/api/chatbot/mensagem"), tokenDe(registrar("Outro")), pergunta("Oi"), 200);
    }
}
