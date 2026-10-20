package br.senai.sportfy.integracao;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.fasterxml.jackson.databind.node.ObjectNode;

/** Cadastro de locais e quadras e as regras de "dono". */
class LocalQuadraIntegracaoTest extends IntegracaoBase {

    @Test
    void local_criarComCoordenadas_eVerDetalheComQuadras() throws Exception {
        String token = tokenDe(registrar("Dono"));
        String nome = "Parque " + unico();
        String localId = criarLocal(token, novoLocal(nome, -23.5874, -46.6576));
        criarQuadra(token, localId, FUTSAL);

        mvc.perform(get("/api/locais/" + localId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value(nome))
                .andExpect(jsonPath("$.latitude").value(-23.5874))
                .andExpect(jsonPath("$.longitude").value(-46.6576))
                .andExpect(jsonPath("$.dono.nome").value("Dono"))
                .andExpect(jsonPath("$.quadras.length()").value(1))
                .andExpect(jsonPath("$.quadras[0].esporte.nome").value("Futsal"));
    }

    @Test
    void local_buscaPorTextoEPorProximidade() throws Exception {
        String token = tokenDe(registrar("Busca"));
        String marca = unico();
        criarLocal(token, novoLocal("Perto " + marca, -23.5614, -46.6559));
        criarLocal(token, novoLocal("Longe " + marca, -22.9711, -43.1822));

        mvc.perform(get("/api/locais").param("texto", marca))
                .andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get("/api/locais").param("texto", marca)
                        .param("latitude", "-23.5505").param("longitude", "-46.6333").param("raioKm", "30"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nome").value("Perto " + marca))
                .andExpect(jsonPath("$[0].distanciaKm").isNumber());
    }

    @Test
    void local_soDonoEdita_eLatitudeSemLongitudeEhRecusada() throws Exception {
        String dono = tokenDe(registrar("Dono2"));
        String outro = tokenDe(registrar("Outro"));
        String localId = criarLocal(dono, novoLocal("Clube " + unico(), null, null));

        ObjectNode edicao = novoLocal("Clube Novo Nome", null, null);
        mvc.perform(put("/api/locais/" + localId).header("Authorization", bearer(outro))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(edicao)))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/locais/" + localId).header("Authorization", bearer(dono))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(edicao)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Clube Novo Nome"));

        ObjectNode metade = novoLocal("Sem longitude", null, null);
        metade.put("latitude", -23.5);
        mvc.perform(post("/api/locais").header("Authorization", bearer(dono))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(metade)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void quadra_soDonoDoLocalCadastra_eFiltroPorEsporte() throws Exception {
        String dono = tokenDe(registrar("Dono3"));
        String outro = tokenDe(registrar("Outro3"));
        String localId = criarLocal(dono, novoLocal("Clube " + unico(), null, null));

        ObjectNode quadra = json.createObjectNode();
        quadra.put("localId", localId);
        quadra.put("esporteId", VOLEI);
        quadra.put("nome", "Quadra de areia");
        mvc.perform(post("/api/quadras").header("Authorization", bearer(outro))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(quadra)))
                .andExpect(status().isForbidden());

        criarQuadra(dono, localId, VOLEI);
        criarQuadra(dono, localId, FUTSAL);
        mvc.perform(get("/api/quadras").param("localId", localId).param("esporteId", String.valueOf(VOLEI)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].esporte.nome").value("Vôlei"))
                .andExpect(jsonPath("$[0].precoHora").value(100.0));
    }

    @Test
    void quadra_esporteInexistente_devolve404_ePrecoNegativo400() throws Exception {
        String dono = tokenDe(registrar("Dono4"));
        String localId = criarLocal(dono, novoLocal("Clube " + unico(), null, null));
        ObjectNode quadra = json.createObjectNode();
        quadra.put("localId", localId);
        quadra.put("esporteId", 9999);
        quadra.put("nome", "Quadra X");
        mvc.perform(post("/api/quadras").header("Authorization", bearer(dono))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(quadra)))
                .andExpect(status().isNotFound());

        quadra.put("esporteId", FUTSAL);
        quadra.put("precoHora", -5);
        mvc.perform(post("/api/quadras").header("Authorization", bearer(dono))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(quadra)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo", hasItem("precoHora")));
    }

    @Test
    void excluir_quadraComPartida_devolve409_semPartidaFunciona() throws Exception {
        String dono = tokenDe(registrar("Dono5"));
        String localId = criarLocal(dono, novoLocal("Clube " + unico(), null, null));
        String comPartida = criarQuadra(dono, localId, FUTSAL);
        String semPartida = criarQuadra(dono, localId, FUTSAL);
        criarPartida(dono, novaPartida(comPartida, 9, 10));

        mvc.perform(delete("/api/quadras/" + comPartida).header("Authorization", bearer(dono)))
                .andExpect(status().isConflict());
        mvc.perform(delete("/api/locais/" + localId).header("Authorization", bearer(dono)))
                .andExpect(status().isConflict());
        mvc.perform(delete("/api/quadras/" + semPartida).header("Authorization", bearer(dono)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/quadras").param("localId", localId))
                .andExpect(jsonPath("$[*].id", not(hasItem(semPartida))));
    }

    @Test
    void excluir_localSemPartidas_apagaAsQuadrasJunto() throws Exception {
        String dono = tokenDe(registrar("Dono6"));
        String localId = criarLocal(dono, novoLocal("Clube " + unico(), null, null));
        String quadraId = criarQuadra(dono, localId, FUTSAL);

        mvc.perform(delete("/api/locais/" + localId).header("Authorization", bearer(dono)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/locais/" + localId)).andExpect(status().isNotFound());
        mvc.perform(get("/api/quadras/" + quadraId)).andExpect(status().isNotFound());
    }

    @Test
    void criarLocal_semLogin_devolve401() throws Exception {
        mvc.perform(post("/api/locais").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(novoLocal("X", null, null))))
                .andExpect(status().isUnauthorized());
    }
}
