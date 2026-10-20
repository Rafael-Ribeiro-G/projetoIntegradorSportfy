package br.senai.sportfy.integracao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/** Confere o que a API grava nas tabelas do Enzo (formato dos valores e regras de exclusão). */
class BancoIntegracaoTest extends IntegracaoBase {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void gravaNoFormatoDoBancoDoEnzo() throws Exception {
        String token = tokenDe(registrar("Formato"));
        String localId = criarLocal(token, novoLocal("Formato " + unico(), -23.5874, -46.6576));
        String partidaId = criarPartida(token, novaPartida(criarQuadra(token, localId, FUTSAL), 10, 10));

        // point guardado como (longitude,latitude)
        String coordenadas = jdbc.queryForObject("select cast(coordenadas as varchar) from locais where id = cast(? as uuid)",
                String.class, localId);
        assertThat(coordenadas).isEqualTo("(-46.6576,-23.5874)");

        // status em minúsculas, como o padrão da tabela
        Map<String, Object> partida = jdbc.queryForMap("select status from partidas where id = cast(? as uuid)", partidaId);
        assertThat(partida.get("status")).isEqualTo("aberta");
        String confirmacao = jdbc.queryForObject(
                "select status_confirmacao from partida_participantes where partida_id = cast(? as uuid)",
                String.class, partidaId);
        assertThat(confirmacao).isEqualTo("confirmado");
    }

    @Test
    void excluirConta_apagaPartidasCriadas_eLocalFicaSemDono() throws Exception {
        JsonNode registro = registrar("Sai");
        String token = tokenDe(registro);
        String usuarioId = registro.get("usuario").get("id").asText();
        String localId = criarLocal(token, novoLocal("Fica " + unico(), null, null));
        String partidaId = criarPartida(token, novaPartida(criarQuadra(token, localId, FUTSAL), 11, 10));

        ObjectNode body = json.createObjectNode();
        body.put("senha", SENHA_PADRAO);
        enviar(post("/api/usuarios/me/exclusao"), token, body, 204);

        assertThat(jdbc.queryForObject("select count(*) from usuarios where id = cast(? as uuid)", Integer.class,
                usuarioId)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from partidas where id = cast(? as uuid)", Integer.class,
                partidaId)).isZero();
        assertThat(jdbc.queryForObject("select dono_id from locais where id = cast(? as uuid)", Object.class,
                localId)).isNull();
    }

    @Test
    void excluirConta_deQuemOrganizaCampeonato_devolve409() throws Exception {
        JsonNode registro = registrar("Campeao");
        String usuarioId = registro.get("usuario").get("id").asText();
        jdbc.update("insert into campeonatos (nome, esporte_id, organizador_id, data_inicio, data_fim) "
                + "values ('Copa', 1, cast(? as uuid), current_date, current_date)", usuarioId);

        ObjectNode body = json.createObjectNode();
        body.put("senha", SENHA_PADRAO);
        JsonNode erro = enviar(post("/api/usuarios/me/exclusao"), tokenDe(registro), body, 409);
        assertThat(erro.get("mensagem").asText()).contains("campeonatos");
        assertThat(jdbc.queryForObject("select count(*) from usuarios where id = cast(? as uuid)", Integer.class,
                usuarioId)).isOne();
    }
}
