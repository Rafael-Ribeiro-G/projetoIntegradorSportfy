package br.senai.sportfy.integracao;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** Confere que o Flyway criou tudo no schema próprio "sportfy" (e nada no public). */
class BancoIntegracaoTest extends IntegracaoBase {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void tabelasFicamNoSchemaSportfy() {
        List<String> tabelas = jdbc.queryForList(
                "select lower(table_name) from information_schema.tables where lower(table_schema) = 'sportfy'",
                String.class);

        assertThat(tabelas).contains("usuarios", "esportes", "usuario_esportes", "eventos", "participacoes",
                "flyway_schema_history");
    }

    @Test
    void nadaFoiCriadoNoSchemaPublic() {
        Integer noPublic = jdbc.queryForObject(
                "select count(*) from information_schema.tables where lower(table_schema) = 'public'",
                Integer.class);
        assertThat(noPublic).isZero();
    }
}
