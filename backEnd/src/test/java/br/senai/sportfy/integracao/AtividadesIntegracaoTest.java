package br.senai.sportfy.integracao;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/** Modelo novo: atividades sem quadra, tipos de local, locais privados e perfil completo. */
class AtividadesIntegracaoTest extends IntegracaoBase {

    @Test
    void corridaNoParque_semQuadra_comTituloEImagem() throws Exception {
        String token = tokenDe(registrar("Corredor"));
        ObjectNode parque = novoLocal("Parque " + unico(), -23.5874, -46.6576);
        parque.put("tipo", "PARQUE");
        String parqueId = criarLocal(token, parque);

        ObjectNode corrida = novaAtividade(parqueId, CORRIDA, 2, "Corrida 5 km");
        corrida.put("descricao", "Ritmo leve, saída do portão 3");
        corrida.put("imagemUrl", "https://exemplo.com/capa.jpg");
        String id = criarPartida(token, corrida);

        mvc.perform(get("/api/partidas/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Corrida 5 km"))
                .andExpect(jsonPath("$.descricao").value("Ritmo leve, saída do portão 3"))
                .andExpect(jsonPath("$.imagemUrl").value("https://exemplo.com/capa.jpg"))
                .andExpect(jsonPath("$.esporte.nome").value("Corrida"))
                .andExpect(jsonPath("$.local.tipo").value("PARQUE"))
                .andExpect(jsonPath("$.quadra").doesNotExist())
                // Corrida não tem quantidade padrão: sem limite de vagas
                .andExpect(jsonPath("$.vagasRestantes").doesNotExist());

        mvc.perform(get("/api/partidas").param("texto", "Corrida 5 km").param("tipoLocal", "PARQUE"))
                .andExpect(jsonPath("$.conteudo[*].id", hasItem(id)));
    }

    @Test
    void semQuadraESemLocal_ouQuadraDeOutroEsporte_devolve400() throws Exception {
        String token = tokenDe(registrar("Confuso"));
        ObjectNode semNada = novaAtividade(null, CORRIDA, 2, "Sem local");
        semNada.remove("localId");
        mvc.perform(post("/api/partidas").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(semNada)))
                .andExpect(status().isBadRequest());

        ObjectNode errado = novaPartida(novaQuadra(token, FUTSAL), 3, 10);
        errado.put("esporteId", VOLEI);
        mvc.perform(post("/api/partidas").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(errado)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("A quadra informada é de outro esporte (Futsal)."));
    }

    @Test
    void imagemComLinkInvalido_devolve400() throws Exception {
        String token = tokenDe(registrar("Img"));
        String localId = criarLocal(token, novoLocal("Academia " + unico(), null, null));
        ObjectNode body = novaAtividade(localId, CORRIDA, 2, "Treino");
        body.put("imagemUrl", "javascript:alert(1)");
        mvc.perform(post("/api/partidas").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo", hasItem("imagemUrl")));
    }

    @Test
    void localPrivado_ficaForaDasBuscasDosOutros_masQuemTemOLinkVe() throws Exception {
        String dono = tokenDe(registrar("Anfitriao"));
        String convidado = tokenDe(registrar("Convidado"));
        String estranho = tokenDe(registrar("Estranho"));
        String marca = unico();

        ObjectNode casa = novoLocal("Casa " + marca, null, null);
        casa.put("publico", false);
        String casaId = criarLocal(dono, casa);
        String quadraId = criarQuadra(dono, casaId, FUTSAL);
        String partidaId = criarPartida(dono, novaPartida(quadraId, 5, 10));

        // o dono vê o local na busca; outros e visitantes não
        mvc.perform(get("/api/locais").param("texto", marca).header("Authorization", bearer(dono)))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].publico").value(false));
        mvc.perform(get("/api/locais").param("texto", marca).header("Authorization", bearer(estranho)))
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/locais").param("texto", marca))
                .andExpect(jsonPath("$.length()").value(0));

        // a partida também some das buscas e das sugestões de quem não está nela
        mvc.perform(get("/api/partidas").param("texto", marca))
                .andExpect(jsonPath("$.totalElementos").value(0));
        mvc.perform(get("/api/partidas/sugestoes").param("limite", "20").header("Authorization", bearer(estranho)))
                .andExpect(jsonPath("$[*].id", not(hasItem(partidaId))));

        // quem recebeu o link (id) vê e entra; aí passa a ver na busca
        mvc.perform(get("/api/partidas/" + partidaId)).andExpect(status().isOk());
        mvc.perform(post("/api/partidas/" + partidaId + "/participacao").header("Authorization", bearer(convidado)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/partidas").param("texto", marca).header("Authorization", bearer(convidado)))
                .andExpect(jsonPath("$.totalElementos").value(1));
        mvc.perform(get("/api/partidas").param("texto", marca).header("Authorization", bearer(estranho)))
                .andExpect(jsonPath("$.totalElementos").value(0));
    }

    @Test
    void perfil_fotoBioEFavoritos_eSugestoesPriorizamFavoritos() throws Exception {
        String organizador = tokenDe(registrar("Org"));
        JsonNode fa = registrar("FaDeVolei");
        String tokenFa = tokenDe(fa);

        String localId = criarLocal(organizador, novoLocal("Clube " + unico(), null, null));
        String idVolei = criarPartida(organizador, novaPartida(criarQuadra(organizador, localId, VOLEI), 6, 12));
        criarPartida(organizador, novaPartida(criarQuadra(organizador, localId, FUTSAL), 6, 10));

        ObjectNode perfil = json.createObjectNode();
        perfil.put("nome", "Fã de Vôlei");
        perfil.put("fotoUrl", "https://exemplo.com/eu.png");
        perfil.put("bio", "Jogo vôlei aos sábados");
        perfil.putArray("esportesFavoritosIds").add(VOLEI);
        mvc.perform(put("/api/usuarios/me").header("Authorization", bearer(tokenFa))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(perfil)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fotoUrl").value("https://exemplo.com/eu.png"))
                .andExpect(jsonPath("$.bio").value("Jogo vôlei aos sábados"))
                .andExpect(jsonPath("$.esportesFavoritos[0].nome").value("Vôlei"));

        // perfil público mostra foto, bio e favoritos, sem e-mail
        mvc.perform(get("/api/usuarios/" + fa.get("usuario").get("id").asText()).header("Authorization", bearer(organizador)))
                .andExpect(jsonPath("$.bio").value("Jogo vôlei aos sábados"))
                .andExpect(jsonPath("$.esportesFavoritos[0].nome").value("Vôlei"))
                .andExpect(jsonPath("$.email").doesNotExist());

        mvc.perform(get("/api/partidas/sugestoes").param("limite", "20").header("Authorization", bearer(tokenFa)))
                .andExpect(jsonPath("$[*].id", hasItem(idVolei)))
                .andExpect(jsonPath("$[*].esporte.nome", not(hasItem("Futsal"))));
    }

    @Test
    void rotasTambemRespondemEmApiEventos() throws Exception {
        String token = tokenDe(registrar("Alias"));
        String localId = criarLocal(token, novoLocal("Pista " + unico(), null, null));
        ObjectNode body = novaAtividade(localId, CORRIDA, 3, "Treino de pista");
        String id = enviar(post("/api/eventos"), token, body, 201).get("id").asText();

        mvc.perform(get("/api/eventos/" + id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Treino de pista"));
        mvc.perform(get("/api/partidas/" + id)).andExpect(status().isOk());
        mvc.perform(get("/api/eventos/sugestoes")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/eventos/sugestoes").header("Authorization", bearer(token))).andExpect(status().isOk());
    }

    @Test
    void cadastro_jaAceitaEsportesFavoritos_eEsporteInexistenteDa404() throws Exception {
        ObjectNode body = novoRegistro("Multi", "multi-" + unico() + "@teste.com");
        body.putArray("esportesFavoritosIds").add(FUTSAL).add(CORRIDA);
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuario.esportesFavoritos.length()").value(2));

        ObjectNode ruim = novoRegistro("Ruim", "ruim-" + unico() + "@teste.com");
        ruim.putArray("esportesFavoritosIds").add(9999);
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(ruim)))
                .andExpect(status().isNotFound());
    }
}
