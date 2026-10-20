package br.senai.sportfy.integracao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/** Upload de imagens e fotoUrl nas respostas de participantes. */
class UploadIntegracaoTest extends IntegracaoBase {

    /** PNG 1x1 válido. */
    private static final byte[] PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==");

    private JsonNode enviarImagem(String token, MockMultipartFile arquivo, int statusEsperado) throws Exception {
        MvcResult res = mvc.perform(multipart("/api/uploads/imagens").file(arquivo)
                        .header("Authorization", bearer(token)))
                .andExpect(status().is(statusEsperado)).andReturn();
        String corpo = res.getResponse().getContentAsString();
        return corpo.isEmpty() ? json.createObjectNode() : json.readTree(corpo);
    }

    @Test
    void enviaPng_eImagemFicaPublicaNaUrlDevolvida() throws Exception {
        String token = tokenDe(registrar("Fotografo"));
        JsonNode r = enviarImagem(token, new MockMultipartFile("arquivo", "eu.png", "image/png", PNG), 201);

        assertThat(r.get("tipo").asText()).isEqualTo("image/png");
        assertThat(r.get("tamanhoBytes").asLong()).isEqualTo(PNG.length);
        String url = r.get("url").asText();
        assertThat(url).endsWith("/uploads/" + r.get("nome").asText());

        // sem login: qualquer um vê a imagem
        mvc.perform(get("/uploads/" + r.get("nome").asText()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"))
                .andExpect(content().bytes(PNG));

        // a url devolvida é aceita como fotoUrl do perfil
        ObjectNode perfil = json.createObjectNode();
        perfil.put("nome", "Fotografo");
        perfil.put("fotoUrl", url);
        enviar(put("/api/usuarios/me"), token, perfil, 200);
    }

    @Test
    void arquivoQueNaoEImagem_mesmoComNomePng_devolve400() throws Exception {
        String token = tokenDe(registrar("Espertinho"));
        enviarImagem(token, new MockMultipartFile("arquivo", "virus.png", "image/png", "<script>".getBytes()), 400);
        enviarImagem(token, new MockMultipartFile("arquivo", "vazio.png", "image/png", new byte[0]), 400);
    }

    @Test
    void uploadSemLogin_devolve401() throws Exception {
        mvc.perform(multipart("/api/uploads/imagens").file(new MockMultipartFile("arquivo", "a.png", "image/png", PNG)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void nomeInvalidoOuInexistente_devolve404() throws Exception {
        mvc.perform(get("/uploads/qualquer.txt")).andExpect(status().isNotFound());
        // barra codificada: o próprio Spring Security já recusa (400)
        mvc.perform(get("/uploads/..%2Fapplication.yml")).andExpect(status().is4xxClientError());
        mvc.perform(get("/uploads/00000000-0000-0000-0000-000000000000.png")).andExpect(status().isNotFound());
    }

    @Test
    void participantesECriador_trazemFotoUrl() throws Exception {
        String tokenCriador = tokenDe(registrar("Capitao"));
        ObjectNode perfil = json.createObjectNode();
        perfil.put("nome", "Capitao");
        perfil.put("fotoUrl", "https://exemplo.com/capitao.png");
        enviar(put("/api/usuarios/me"), tokenCriador, perfil, 200);

        String partidaId = criarPartida(tokenCriador, novaPartida(novaQuadra(tokenCriador, FUTSAL), 4, 10));
        String tokenJogador = tokenDe(registrar("Jogador"));
        enviar(post("/api/partidas/" + partidaId + "/participacao"), tokenJogador, null, 200);

        mvc.perform(get("/api/partidas/" + partidaId + "/participantes").header("Authorization", bearer(tokenJogador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.criador == true)].fotoUrl").value("https://exemplo.com/capitao.png"))
                .andExpect(jsonPath("$.length()").value(2));

        mvc.perform(get("/api/partidas/" + partidaId))
                .andExpect(jsonPath("$.criador.fotoUrl").value("https://exemplo.com/capitao.png"));
    }
}
