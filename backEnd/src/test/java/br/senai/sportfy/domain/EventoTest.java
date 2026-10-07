package br.senai.sportfy.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import br.senai.sportfy.domain.enums.SituacaoEvento;
import br.senai.sportfy.domain.enums.StatusEvento;

class EventoTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 10, 7, 12, 0);

    private Evento evento(LocalDateTime inicio, int duracao, int max, StatusEvento status) {
        Evento e = new Evento();
        e.setDataHoraInicio(inicio);
        e.setDuracaoMinutos(duracao);
        e.setMaxParticipantes(max);
        e.setStatus(status);
        return e;
    }

    @Test
    void situacao_abertoQuandoFuturoEComVaga() {
        Evento e = evento(AGORA.plusHours(2), 60, 10, StatusEvento.ATIVO);
        assertThat(e.calcularSituacao(AGORA, 3)).isEqualTo(SituacaoEvento.ABERTO);
    }

    @Test
    void situacao_lotadoQuandoAtingeLimite() {
        Evento e = evento(AGORA.plusHours(2), 60, 10, StatusEvento.ATIVO);
        assertThat(e.calcularSituacao(AGORA, 10)).isEqualTo(SituacaoEvento.LOTADO);
    }

    @Test
    void situacao_emAndamentoDuranteOJogo() {
        Evento e = evento(AGORA.minusMinutes(30), 60, 10, StatusEvento.ATIVO);
        assertThat(e.calcularSituacao(AGORA, 3)).isEqualTo(SituacaoEvento.EM_ANDAMENTO);
    }

    @Test
    void situacao_encerradoDepoisDoFim() {
        Evento e = evento(AGORA.minusHours(3), 60, 10, StatusEvento.ATIVO);
        assertThat(e.calcularSituacao(AGORA, 3)).isEqualTo(SituacaoEvento.ENCERRADO);
    }

    @Test
    void situacao_canceladoTemPrioridade() {
        Evento e = evento(AGORA.plusHours(2), 60, 10, StatusEvento.CANCELADO);
        assertThat(e.calcularSituacao(AGORA, 3)).isEqualTo(SituacaoEvento.CANCELADO);
    }
}
