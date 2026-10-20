package br.senai.sportfy.integracao;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Base dos testes de integração: sobe a aplicação inteira (rotas + segurança + banco H2 com a
 * mesma estrutura do banco do Enzo) e faz requisições HTTP simuladas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class IntegracaoBase {

    protected static final String SENHA_PADRAO = "Senha1234";
    /** Ids fixos de esportes-teste.sql. */
    protected static final int FUTEBOL = 1;
    protected static final int FUTSAL = 2;
    protected static final int VOLEI = 3;
    protected static final int CORRIDA = 5;

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper json;

    /** Sufixo único para não haver conflito de e-mails/nomes entre testes. */
    protected static String unico() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    protected ObjectNode novoRegistro(String nome, String email) {
        ObjectNode body = json.createObjectNode();
        body.put("nome", nome);
        body.put("email", email);
        body.put("senha", SENHA_PADRAO);
        body.put("nivelHabilidade", 3);
        return body;
    }

    /** Cadastra um usuário novo e devolve o JSON de resposta (token + usuario). */
    protected JsonNode registrar(String nome) throws Exception {
        String email = nome.toLowerCase() + "-" + unico() + "@teste.com";
        return enviar(post("/api/auth/registro"), null, novoRegistro(nome, email), 201);
    }

    protected String tokenDe(JsonNode registro) {
        return registro.get("token").asText();
    }

    protected String login(String email, String senha) throws Exception {
        ObjectNode body = json.createObjectNode();
        body.put("email", email);
        body.put("senha", senha);
        return enviar(post("/api/auth/login"), null, body, 200).get("token").asText();
    }

    protected static String bearer(String token) {
        return "Bearer " + token;
    }

    protected ObjectNode novoLocal(String nome, Double latitude, Double longitude) {
        ObjectNode body = json.createObjectNode();
        body.put("nome", nome);
        body.put("endereco", "Rua Exemplo, 100 - São Paulo/SP");
        if (latitude != null) {
            body.put("latitude", latitude);
            body.put("longitude", longitude);
        }
        return body;
    }

    protected String criarLocal(String token, ObjectNode body) throws Exception {
        return enviar(post("/api/locais"), token, body, 201).get("id").asText();
    }

    protected String criarQuadra(String token, String localId, int esporteId) throws Exception {
        ObjectNode body = json.createObjectNode();
        body.put("localId", localId);
        body.put("esporteId", esporteId);
        body.put("nome", "Quadra " + unico());
        body.put("precoHora", 100.00);
        return enviar(post("/api/quadras"), token, body, 201).get("id").asText();
    }

    /** Local + quadra novos (sem coordenadas), para quem só precisa de uma quadra. */
    protected String novaQuadra(String token, int esporteId) throws Exception {
        String localId = criarLocal(token, novoLocal("Clube " + unico(), null, null));
        return criarQuadra(token, localId, esporteId);
    }

    /** Corpo válido de partida daqui a {@code dias} dias, das 19h às 20h30. */
    protected ObjectNode novaPartida(String quadraId, int dias, Integer maxJogadores) {
        LocalDateTime inicio = LocalDateTime.now().plusDays(dias).withHour(19).withMinute(0).withSecond(0).withNano(0);
        ObjectNode body = json.createObjectNode();
        body.put("quadraId", quadraId);
        body.put("dataHoraInicio", inicio.toString());
        body.put("dataHoraFim", inicio.plusMinutes(90).toString());
        if (maxJogadores != null) {
            body.put("maxJogadores", maxJogadores);
        }
        return body;
    }

    /** Atividade sem quadra (corrida no parque, treino na academia...). */
    protected ObjectNode novaAtividade(String localId, int esporteId, int dias, String titulo) {
        LocalDateTime inicio = LocalDateTime.now().plusDays(dias).withHour(7).withMinute(0).withSecond(0).withNano(0);
        ObjectNode body = json.createObjectNode();
        body.put("localId", localId);
        body.put("esporteId", esporteId);
        body.put("titulo", titulo);
        body.put("dataHoraInicio", inicio.toString());
        body.put("dataHoraFim", inicio.plusMinutes(60).toString());
        return body;
    }

    protected String criarPartida(String token, ObjectNode body) throws Exception {
        return enviar(post("/api/partidas"), token, body, 201).get("id").asText();
    }

    /** Faz a requisição, confere o status e devolve o JSON da resposta. */
    protected JsonNode enviar(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder req,
                              String token, Object body, int statusEsperado) throws Exception {
        if (token != null) {
            req.header("Authorization", bearer(token));
        }
        if (body != null) {
            req.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        }
        MvcResult res = mvc.perform(req).andExpect(status().is(statusEsperado)).andReturn();
        String conteudo = res.getResponse().getContentAsString();
        return conteudo.isEmpty() ? json.createObjectNode() : json.readTree(conteudo);
    }
}
