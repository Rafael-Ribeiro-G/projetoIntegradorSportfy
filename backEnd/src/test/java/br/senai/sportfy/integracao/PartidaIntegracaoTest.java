package br.senai.sportfy.integracao;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.fasterxml.jackson.databind.node.ObjectNode;

/** Fluxo completo de partidas passando pelas rotas, segurança e banco. */
class PartidaIntegracaoTest extends IntegracaoBase {

    @Test
    void fluxoCompleto_criarParticiparSairECancelar() throws Exception {
        String criador = tokenDe(registrar("Org"));
        String jogador = tokenDe(registrar("Jogador"));
        String intruso = tokenDe(registrar("Intruso"));
        String nomeLocal = "Arena " + unico();
        String localId = criarLocal(criador, novoLocal(nomeLocal, null, null));
        String quadraId = criarQuadra(criador, localId, FUTSAL);

        String id = criarPartida(criador, novaPartida(quadraId, 3, 10));

        // visitante (sem login) consegue ver a lista e o detalhe
        mvc.perform(get("/api/partidas").param("texto", nomeLocal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.conteudo[0].local.nome").value(nomeLocal))
                .andExpect(jsonPath("$.conteudo[0].esporte.nome").value("Futsal"))
                .andExpect(jsonPath("$.conteudo[0].participantesConfirmados").value(1))
                .andExpect(jsonPath("$.conteudo[0].situacao").value("ABERTA"))
                .andExpect(jsonPath("$.conteudo[0].usuarioParticipa").doesNotExist());

        // jogador entra
        mvc.perform(post("/api/partidas/" + id + "/participacao").header("Authorization", bearer(jogador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participantesConfirmados").value(2))
                .andExpect(jsonPath("$.vagasRestantes").value(8))
                .andExpect(jsonPath("$.usuarioParticipa").value(true));

        // não pode entrar duas vezes
        mvc.perform(post("/api/partidas/" + id + "/participacao").header("Authorization", bearer(jogador)))
                .andExpect(status().isConflict());

        // quem não criou não edita nem cancela
        mvc.perform(put("/api/partidas/" + id).header("Authorization", bearer(intruso))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(novaPartida(quadraId, 3, 20))))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/partidas/" + id).header("Authorization", bearer(intruso)))
                .andExpect(status().isForbidden());

        // lista de participantes (precisa estar logado); quem criou vem primeiro
        mvc.perform(get("/api/partidas/" + id + "/participantes"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/partidas/" + id + "/participantes").header("Authorization", bearer(criador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].criador").value(true))
                .andExpect(jsonPath("$[0].email").doesNotExist());

        // criador edita (mesmo horário: não conflita com a própria partida)
        mvc.perform(put("/api/partidas/" + id).header("Authorization", bearer(criador))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(novaPartida(quadraId, 3, 12))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxJogadores").value(12));

        // aparece em "minhas partidas" do jogador
        mvc.perform(get("/api/usuarios/me/partidas").header("Authorization", bearer(jogador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participando[*].id", hasItem(id)));

        // jogador sai
        mvc.perform(delete("/api/partidas/" + id + "/participacao").header("Authorization", bearer(jogador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participantesConfirmados").value(1));

        // criador cancela
        mvc.perform(delete("/api/partidas/" + id).header("Authorization", bearer(criador)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/partidas/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADA"))
                .andExpect(jsonPath("$.situacao").value("CANCELADA"));

        // cancelada some da busca padrão e ninguém entra nela
        mvc.perform(get("/api/partidas").param("texto", nomeLocal))
                .andExpect(jsonPath("$.totalElementos").value(0));
        mvc.perform(post("/api/partidas/" + id + "/participacao").header("Authorization", bearer(intruso)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void partidaLotada_recusaNovoParticipante() throws Exception {
        String criador = tokenDe(registrar("Org2"));
        String segundo = tokenDe(registrar("Segundo"));
        String terceiro = tokenDe(registrar("Terceiro"));
        String id = criarPartida(criador, novaPartida(novaQuadra(criador, FUTSAL), 2, 2));

        mvc.perform(post("/api/partidas/" + id + "/participacao").header("Authorization", bearer(segundo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("LOTADA"));
        mvc.perform(post("/api/partidas/" + id + "/participacao").header("Authorization", bearer(terceiro)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem").value("Partida lotada: não há mais vagas."));
    }

    @Test
    void mesmaQuadraNoMesmoHorario_devolve409_masOutraQuadraPode() throws Exception {
        String token = tokenDe(registrar("Org3"));
        String localId = criarLocal(token, novoLocal("Clube " + unico(), null, null));
        String quadra1 = criarQuadra(token, localId, FUTSAL);
        String quadra2 = criarQuadra(token, localId, FUTSAL);
        criarPartida(token, novaPartida(quadra1, 4, 10));

        mvc.perform(post("/api/partidas").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(novaPartida(quadra1, 4, 10))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Esta quadra já tem uma partida marcada nesse horário."));
        criarPartida(token, novaPartida(quadra2, 4, 10));
    }

    @Test
    void semMaxJogadores_usaPadraoDoEsporte() throws Exception {
        String token = tokenDe(registrar("Org4"));
        String id = criarPartida(token, novaPartida(novaQuadra(token, VOLEI), 5, null));
        mvc.perform(get("/api/partidas/" + id))
                .andExpect(jsonPath("$.maxJogadores").value(12))
                .andExpect(jsonPath("$.vagasRestantes").value(11));
    }

    @Test
    void criarPartida_semLogin_devolve401() throws Exception {
        mvc.perform(post("/api/partidas").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(novaPartida("00000000-0000-0000-0000-000000000001", 2, 10))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void criarPartida_dataPassadaEVagasInvalidas_devolve400() throws Exception {
        String token = tokenDe(registrar("Org5"));
        ObjectNode body = novaPartida(novaQuadra(token, FUTSAL), 2, 1);
        body.put("dataHoraInicio", LocalDateTime.now().minusDays(1).withNano(0).toString());

        mvc.perform(post("/api/partidas").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo", hasItem("dataHoraInicio")))
                .andExpect(jsonPath("$.campos[*].campo", hasItem("maxJogadores")));
    }

    @Test
    void criarPartida_quadraInexistente_devolve404() throws Exception {
        String token = tokenDe(registrar("Org6"));
        mvc.perform(post("/api/partidas").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(novaPartida("00000000-0000-0000-0000-000000000001", 2, 10))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Quadra não encontrada."));
    }

    @Test
    void busca_porProximidade_trazSoPartidasDentroDoRaio() throws Exception {
        String token = tokenDe(registrar("Geo"));
        String marca = unico();

        String perto = criarLocal(token, novoLocal("Perto " + marca, -23.5614, -46.6559));   // Av. Paulista
        String longe = criarLocal(token, novoLocal("Longe " + marca, -22.9711, -43.1822));   // Copacabana
        criarPartida(token, novaPartida(criarQuadra(token, perto, FUTEBOL), 6, 10));
        criarPartida(token, novaPartida(criarQuadra(token, longe, FUTEBOL), 6, 10));

        mvc.perform(get("/api/partidas")
                        .param("texto", marca)
                        .param("latitude", "-23.5505")  // Praça da Sé
                        .param("longitude", "-46.6333")
                        .param("raioKm", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.conteudo[0].local.nome").value("Perto " + marca))
                .andExpect(jsonPath("$.conteudo[0].distanciaKm").isNumber());
    }

    @Test
    void busca_porEsporte() throws Exception {
        String token = tokenDe(registrar("Filtro"));
        String nomeLocal = "Filtro " + unico();
        String localId = criarLocal(token, novoLocal(nomeLocal, null, null));
        criarPartida(token, novaPartida(criarQuadra(token, localId, VOLEI), 7, 10));
        criarPartida(token, novaPartida(criarQuadra(token, localId, FUTSAL), 7, 10));

        mvc.perform(get("/api/partidas").param("texto", nomeLocal).param("esporteId", String.valueOf(VOLEI)))
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.conteudo[0].esporte.nome").value("Vôlei"));
    }

    @Test
    void busca_raioSemCoordenadas_eTamanhoAcimaDoLimite_devolvem400() throws Exception {
        mvc.perform(get("/api/partidas").param("raioKm", "10")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/partidas").param("tamanho", "500")).andExpect(status().isBadRequest());
    }

    @Test
    void sugestoes_trazPartidasDosOutrosComVaga() throws Exception {
        String criador = tokenDe(registrar("Org7"));
        String jogador = tokenDe(registrar("Fa"));
        String idVolei = criarPartida(criador, novaPartida(novaQuadra(criador, VOLEI), 8, 12));

        mvc.perform(get("/api/partidas/sugestoes").param("limite", "20").param("esporteId", String.valueOf(VOLEI))
                        .header("Authorization", bearer(jogador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(idVolei)))
                .andExpect(jsonPath("$[*].esporte.nome", not(hasItem("Futsal"))));

        // quem criou não recebe a própria partida como sugestão
        mvc.perform(get("/api/partidas/sugestoes").param("limite", "20").header("Authorization", bearer(criador)))
                .andExpect(jsonPath("$[*].id", not(hasItem(idVolei))));
    }

    @Test
    void sugestoes_semLogin_devolve401() throws Exception {
        mvc.perform(get("/api/partidas/sugestoes")).andExpect(status().isUnauthorized());
    }

    @Test
    void partidaInexistente_eIdInvalido() throws Exception {
        mvc.perform(get("/api/partidas/00000000-0000-0000-0000-000000000099"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Partida não encontrada."));
        mvc.perform(get("/api/partidas/123")).andExpect(status().isBadRequest());
    }
}
