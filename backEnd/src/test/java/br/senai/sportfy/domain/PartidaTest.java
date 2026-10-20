package br.senai.sportfy.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import br.senai.sportfy.domain.enums.SituacaoPartida;
import br.senai.sportfy.domain.enums.StatusPartida;

class PartidaTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 10, 19, 12, 0);

    private Partida partida(LocalDateTime inicio, int minutos, Integer max, StatusPartida status) {
        Partida p = new Partida();
        p.setDataHoraInicio(inicio);
        p.setDataHoraFim(inicio.plusMinutes(minutos));
        p.setMaxJogadores(max);
        p.setStatus(status);
        return p;
    }

    @Test
    void situacao_abertaQuandoFuturaEComVaga() {
        Partida p = partida(AGORA.plusHours(2), 60, 10, StatusPartida.ABERTA);
        assertThat(p.calcularSituacao(AGORA, 3)).isEqualTo(SituacaoPartida.ABERTA);
    }

    @Test
    void situacao_lotadaQuandoAtingeLimite() {
        Partida p = partida(AGORA.plusHours(2), 60, 10, StatusPartida.ABERTA);
        assertThat(p.calcularSituacao(AGORA, 10)).isEqualTo(SituacaoPartida.LOTADA);
    }

    @Test
    void situacao_semLimiteNuncaLota() {
        Partida p = partida(AGORA.plusHours(2), 60, null, StatusPartida.ABERTA);
        assertThat(p.calcularSituacao(AGORA, 500)).isEqualTo(SituacaoPartida.ABERTA);
        assertThat(p.estaLotada(500)).isFalse();
    }

    @Test
    void situacao_emAndamentoDuranteOJogo() {
        Partida p = partida(AGORA.minusMinutes(30), 60, 10, StatusPartida.ABERTA);
        assertThat(p.calcularSituacao(AGORA, 3)).isEqualTo(SituacaoPartida.EM_ANDAMENTO);
    }

    @Test
    void situacao_encerradaDepoisDoFimOuFinalizada() {
        assertThat(partida(AGORA.minusHours(3), 60, 10, StatusPartida.ABERTA).calcularSituacao(AGORA, 3))
                .isEqualTo(SituacaoPartida.ENCERRADA);
        assertThat(partida(AGORA.plusHours(3), 60, 10, StatusPartida.FINALIZADA).calcularSituacao(AGORA, 3))
                .isEqualTo(SituacaoPartida.ENCERRADA);
    }

    @Test
    void situacao_canceladaTemPrioridade() {
        Partida p = partida(AGORA.plusHours(2), 60, 10, StatusPartida.CANCELADA);
        assertThat(p.calcularSituacao(AGORA, 3)).isEqualTo(SituacaoPartida.CANCELADA);
    }

    @Test
    void status_textoDoBancoViraEnum() {
        assertThat(StatusPartida.doBanco("cancelada")).isEqualTo(StatusPartida.CANCELADA);
        assertThat(StatusPartida.doBanco("Aberta")).isEqualTo(StatusPartida.ABERTA);
        assertThat(StatusPartida.doBanco(null)).isEqualTo(StatusPartida.ABERTA);
        assertThat(StatusPartida.doBanco("qualquer")).isEqualTo(StatusPartida.ABERTA);
        assertThat(StatusPartida.CANCELADA.valorNoBanco()).isEqualTo("cancelada");
    }
}
