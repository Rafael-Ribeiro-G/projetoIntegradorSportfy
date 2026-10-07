package br.senai.sportfy.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

class GeoUtilsTest {

    // Praça da Sé (SP) e Cristo Redentor (RJ)
    private static final double SE_LAT = -23.5503, SE_LON = -46.6339;
    private static final double RIO_LAT = -22.9519, RIO_LON = -43.2105;

    @Test
    void distancia_entreSaoPauloERio_aproximadamente360km() {
        double km = GeoUtils.distanciaKm(SE_LAT, SE_LON, RIO_LAT, RIO_LON);
        assertThat(km).isCloseTo(357.0, within(10.0));
    }

    @Test
    void distancia_mesmoPonto_ehZero() {
        assertThat(GeoUtils.distanciaKm(SE_LAT, SE_LON, SE_LAT, SE_LON)).isCloseTo(0.0, within(0.0001));
    }

    @Test
    void caixaDelimitadora_contemOPontoCentral() {
        double[] caixa = GeoUtils.caixaDelimitadora(SE_LAT, SE_LON, 10);
        assertThat(SE_LAT).isBetween(caixa[0], caixa[1]);
        assertThat(SE_LON).isBetween(caixa[2], caixa[3]);
        // 10 km ~ 0,09 grau de latitude
        assertThat(caixa[1] - caixa[0]).isCloseTo(0.18, within(0.01));
    }

    @Test
    void arredondar_umaCasaDecimal() {
        assertThat(GeoUtils.arredondar(3.27)).isEqualTo(3.3);
    }
}
