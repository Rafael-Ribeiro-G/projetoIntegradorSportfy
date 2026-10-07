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
 * Base dos testes de integração: sobe a aplicação inteira (rotas + segurança + banco H2
 * com as mesmas migrations Flyway do PostgreSQL) e faz requisições HTTP simuladas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class IntegracaoBase {

    protected static final String SENHA_PADRAO = "Senha1234";
    protected static final String ADMIN_EMAIL = "admin@sportfy.test";
    protected static final String ADMIN_SENHA = "Admin12345";

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper json;

    /** Sufixo único para não haver conflito de e-mails/títulos entre testes. */
    protected static String unico() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    protected ObjectNode novoRegistro(String nome, String email) {
        ObjectNode body = json.createObjectNode();
        body.put("nome", nome);
        body.put("email", email);
        body.put("senha", SENHA_PADRAO);
        body.put("cidade", "São Paulo");
        body.put("aceiteTermos", true);
        return body;
    }

    /** Cadastra um usuário novo e devolve o JSON de resposta (token + usuario). */
    protected JsonNode registrar(String nome) throws Exception {
        String email = nome.toLowerCase() + "-" + unico() + "@teste.com";
        MvcResult res = mvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(novoRegistro(nome, email))))
                .andExpect(status().isCreated())
                .andReturn();
        return json.readTree(res.getResponse().getContentAsString());
    }

    protected String tokenDe(JsonNode registro) {
        return registro.get("token").asText();
    }

    protected String login(String email, String senha) throws Exception {
        ObjectNode body = json.createObjectNode();
        body.put("email", email);
        body.put("senha", senha);
        MvcResult res = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn();
        return json.readTree(res.getResponse().getContentAsString()).get("token").asText();
    }

    protected static String bearer(String token) {
        return "Bearer " + token;
    }

    /** Corpo válido de criação de evento (Futebol = id 1 nas migrations). */
    protected ObjectNode novoEvento(String titulo, int maxParticipantes) {
        ObjectNode body = json.createObjectNode();
        body.put("titulo", titulo);
        body.put("descricao", "Partida amistosa");
        body.put("esporteId", 1);
        body.put("dataHoraInicio", LocalDateTime.now().plusDays(3).withNano(0).toString());
        body.put("duracaoMinutos", 90);
        body.put("localNome", "Quadra Central");
        body.put("endereco", "Rua Exemplo, 100");
        body.put("cidade", "São Paulo");
        body.put("maxParticipantes", maxParticipantes);
        body.put("nivel", "TODOS");
        return body;
    }

    protected long criarEvento(String token, ObjectNode body) throws Exception {
        MvcResult res = mvc.perform(post("/api/eventos")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn();
        return json.readTree(res.getResponse().getContentAsString()).get("id").asLong();
    }
}
