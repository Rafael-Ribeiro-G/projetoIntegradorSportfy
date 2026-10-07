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

/** Fluxo completo de eventos passando pelas rotas, segurança e banco. */
class EventoIntegracaoTest extends IntegracaoBase {

    @Test
    void fluxoCompleto_criarParticiparSairECancelar() throws Exception {
        String organizador = tokenDe(registrar("Org"));
        String jogador = tokenDe(registrar("Jogador"));
        String intruso = tokenDe(registrar("Intruso"));
        String titulo = "Pelada " + unico();

        long id = criarEvento(organizador, novoEvento(titulo, 10));

        // visitante (sem login) consegue ver a lista e o detalhe
        mvc.perform(get("/api/eventos").param("texto", titulo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo[0].titulo").value(titulo))
                .andExpect(jsonPath("$.conteudo[0].participantesConfirmados").value(1))
                .andExpect(jsonPath("$.conteudo[0].situacao").value("ABERTO"))
                .andExpect(jsonPath("$.conteudo[0].usuarioParticipa").doesNotExist());

        // jogador entra
        mvc.perform(post("/api/eventos/" + id + "/participacao").header("Authorization", bearer(jogador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participantesConfirmados").value(2))
                .andExpect(jsonPath("$.usuarioParticipa").value(true));

        // não pode entrar duas vezes
        mvc.perform(post("/api/eventos/" + id + "/participacao").header("Authorization", bearer(jogador)))
                .andExpect(status().isConflict());

        // quem não é organizador não edita nem cancela
        mvc.perform(put("/api/eventos/" + id).header("Authorization", bearer(intruso))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(novoEvento("Hackeado", 10))))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/eventos/" + id).header("Authorization", bearer(intruso)))
                .andExpect(status().isForbidden());

        // lista de participantes (precisa estar logado)
        mvc.perform(get("/api/eventos/" + id + "/participantes"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/eventos/" + id + "/participantes").header("Authorization", bearer(organizador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].organizador").value(true));

        // organizador edita
        ObjectNode edicao = novoEvento(titulo + " editado", 12);
        mvc.perform(put("/api/eventos/" + id).header("Authorization", bearer(organizador))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(edicao)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxParticipantes").value(12));

        // aparece em "meus eventos" do jogador
        mvc.perform(get("/api/usuarios/me/eventos").header("Authorization", bearer(jogador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participando[*].id", hasItem((int) id)));

        // jogador sai
        mvc.perform(delete("/api/eventos/" + id + "/participacao").header("Authorization", bearer(jogador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participantesConfirmados").value(1));

        // organizador cancela
        mvc.perform(delete("/api/eventos/" + id).header("Authorization", bearer(organizador)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/eventos/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("CANCELADO"));

        // ninguém entra em evento cancelado
        mvc.perform(post("/api/eventos/" + id + "/participacao").header("Authorization", bearer(intruso)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void eventoLotado_recusaNovoParticipante() throws Exception {
        String organizador = tokenDe(registrar("Org2"));
        String segundo = tokenDe(registrar("Segundo"));
        String terceiro = tokenDe(registrar("Terceiro"));

        long id = criarEvento(organizador, novoEvento("Tênis " + unico(), 2));

        mvc.perform(post("/api/eventos/" + id + "/participacao").header("Authorization", bearer(segundo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("LOTADO"));
        mvc.perform(post("/api/eventos/" + id + "/participacao").header("Authorization", bearer(terceiro)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem").value("Evento lotado: não há mais vagas."));
    }

    @Test
    void criarEvento_semLogin_devolve401() throws Exception {
        mvc.perform(post("/api/eventos").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(novoEvento("Sem login", 10))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void criarEvento_comDataPassadaEVagasInvalidas_devolve400() throws Exception {
        String token = tokenDe(registrar("Org3"));
        ObjectNode body = novoEvento("Data errada", 1);
        body.put("dataHoraInicio", LocalDateTime.now().minusDays(1).withNano(0).toString());

        mvc.perform(post("/api/eventos").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo", hasItem("dataHoraInicio")))
                .andExpect(jsonPath("$.campos[*].campo", hasItem("maxParticipantes")));
    }

    @Test
    void criarEvento_esporteInexistente_devolve404() throws Exception {
        String token = tokenDe(registrar("Org4"));
        ObjectNode body = novoEvento("Esporte fantasma", 10);
        body.put("esporteId", 99999);

        mvc.perform(post("/api/eventos").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().isNotFound());
    }

    @Test
    void busca_porProximidade_trazSoEventosDentroDoRaio() throws Exception {
        String token = tokenDe(registrar("Geo"));
        String marca = unico();

        ObjectNode emSaoPaulo = novoEvento("Perto " + marca, 10);
        emSaoPaulo.put("latitude", -23.5614);   // Av. Paulista
        emSaoPaulo.put("longitude", -46.6559);
        criarEvento(token, emSaoPaulo);

        ObjectNode noRio = novoEvento("Longe " + marca, 10);
        noRio.put("cidade", "Rio de Janeiro");
        noRio.put("latitude", -22.9711);        // Copacabana
        noRio.put("longitude", -43.1822);
        criarEvento(token, noRio);

        mvc.perform(get("/api/eventos")
                        .param("texto", marca)
                        .param("latitude", "-23.5505")  // Praça da Sé
                        .param("longitude", "-46.6333")
                        .param("raioKm", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.conteudo[0].titulo").value("Perto " + marca))
                .andExpect(jsonPath("$.conteudo[0].distanciaKm").isNumber());
    }

    @Test
    void busca_raioSemCoordenadas_devolve400() throws Exception {
        mvc.perform(get("/api/eventos").param("raioKm", "10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void busca_tamanhoDePaginaAcimaDoLimite_devolve400() throws Exception {
        mvc.perform(get("/api/eventos").param("tamanho", "500"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sugestoes_trazEventosDoEsporteFavorito() throws Exception {
        String organizador = tokenDe(registrar("Org5"));
        String jogador = tokenDe(registrar("Fa"));
        String marca = unico();

        ObjectNode volei = novoEvento("Vôlei " + marca, 12);
        volei.put("esporteId", 4); // Vôlei nas migrations
        long idVolei = criarEvento(organizador, volei);

        ObjectNode perfil = json.createObjectNode();
        perfil.put("nome", "Fã de vôlei");
        perfil.putArray("esportesFavoritosIds").add(4);
        mvc.perform(put("/api/usuarios/me").header("Authorization", bearer(jogador))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(perfil)))
                .andExpect(status().isOk());

        mvc.perform(get("/api/eventos/sugestoes").param("limite", "20").header("Authorization", bearer(jogador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem((int) idVolei)))
                .andExpect(jsonPath("$[*].esporte.nome", not(hasItem("Futebol"))));
    }

    @Test
    void sugestoes_semLogin_devolve401() throws Exception {
        mvc.perform(get("/api/eventos/sugestoes")).andExpect(status().isUnauthorized());
    }

    @Test
    void eventoInexistente_devolve404() throws Exception {
        mvc.perform(get("/api/eventos/987654"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Evento não encontrado."));
    }
}
