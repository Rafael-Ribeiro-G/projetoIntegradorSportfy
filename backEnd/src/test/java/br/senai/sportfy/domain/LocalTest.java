package br.senai.sportfy.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Conversão da coluna point "(longitude,latitude)" do PostgreSQL. */
class LocalTest {

    @Test
    void definirPosicao_gravaLongitudeDepoisLatitude() {
        Local l = new Local();
        l.definirPosicao(-23.5874, -46.6576);
        assertThat(l.getCoordenadas()).isEqualTo("(-46.6576,-23.5874)");
        assertThat(l.getLatitude()).isEqualTo(-23.5874);
        assertThat(l.getLongitude()).isEqualTo(-46.6576);
    }

    @Test
    void lePontoNoFormatoDoPostgres() {
        Local l = new Local();
        l.setCoordenadas("(-43.1822,-22.9711)");
        assertThat(l.temPosicao()).isTrue();
        assertThat(l.getLatitude()).isEqualTo(-22.9711);
        assertThat(l.getLongitude()).isEqualTo(-43.1822);
    }

    @Test
    void semCoordenadasOuFormatoInvalido_semPosicao() {
        Local l = new Local();
        assertThat(l.temPosicao()).isFalse();
        assertThat(l.getLatitude()).isNull();
        l.setCoordenadas("lixo");
        assertThat(l.temPosicao()).isFalse();
        l.definirPosicao(null, -46.0);
        assertThat(l.getCoordenadas()).isNull();
    }
}
