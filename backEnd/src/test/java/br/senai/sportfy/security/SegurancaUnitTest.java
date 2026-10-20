package br.senai.sportfy.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.senai.sportfy.config.SportfyProperties;
import br.senai.sportfy.domain.Usuario;

/** Testes unitários de JWT e do bloqueio de tentativas de login. */
class SegurancaUnitTest {

    private static final String SEGREDO = "um-segredo-de-teste-com-mais-de-32-caracteres!!";

    private static SportfyProperties props(String segredo, int maxTentativas) {
        return new SportfyProperties(
                new SportfyProperties.Jwt(segredo, 60, "sportfy-api"),
                new SportfyProperties.Cors("http://localhost:3000"),
                new SportfyProperties.Login(maxTentativas, 15),
                new SportfyProperties.Uploads(null));
    }

    private static final UUID ID = UUID.fromString("7f1c2a52-1b6e-4c1e-9d0a-2b9f3c8e4a10");

    private static Usuario usuario() {
        Usuario u = new Usuario();
        u.setId(ID);
        u.setNome("Teste");
        u.setEmail("teste@sportfy.com");
        return u;
    }

    // ------------------------------------------------------------------- JWT

    @Test
    void jwt_tokenGeradoEhValidoEDevolveOId() {
        JwtService jwt = new JwtService(props(SEGREDO, 5));
        String token = jwt.gerarToken(usuario());
        assertThat(jwt.extrairUsuarioId(token)).contains(ID);
    }

    @Test
    void jwt_tokenAdulteradoEhRejeitado() {
        JwtService jwt = new JwtService(props(SEGREDO, 5));
        String token = jwt.gerarToken(usuario());
        String adulterado = token.substring(0, token.length() - 3) + "abc";
        assertThat(jwt.extrairUsuarioId(adulterado)).isEmpty();
    }

    @Test
    void jwt_tokenAssinadoComOutroSegredoEhRejeitado() {
        String token = new JwtService(props("outro-segredo-qualquer-com-mais-de-32-chars", 5)).gerarToken(usuario());
        assertThat(new JwtService(props(SEGREDO, 5)).extrairUsuarioId(token)).isEmpty();
    }

    @Test
    void jwt_lixoEhRejeitado() {
        JwtService jwt = new JwtService(props(SEGREDO, 5));
        assertThat(jwt.extrairUsuarioId("nao-e-um-token")).isEmpty();
        assertThat(jwt.extrairUsuarioId("")).isEmpty();
    }

    @Test
    void jwt_segredoCurtoImpedeSubida() {
        assertThatThrownBy(() -> new JwtService(props("curto", 5))).isInstanceOf(IllegalStateException.class);
    }

    // ----------------------------------------------------- tentativas de login

    @Test
    void login_bloqueiaAposMaximoDeFalhas() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-07T15:00:00Z"), ZoneOffset.UTC);
        LoginAttemptService servico = new LoginAttemptService(props(SEGREDO, 3), clock);

        servico.registrarFalha("Ana@Teste.com");
        servico.registrarFalha("ana@teste.com");
        assertThat(servico.estaBloqueado("ana@teste.com")).isFalse();

        servico.registrarFalha("ana@teste.com ");
        assertThat(servico.estaBloqueado("ana@teste.com")).isTrue();
        assertThat(servico.estaBloqueado("outra@teste.com")).isFalse();
    }

    @Test
    void login_bloqueioExpiraDepoisDoTempo() {
        RelogioAjustavel relogio = new RelogioAjustavel(Instant.parse("2026-10-07T15:00:00Z"));
        LoginAttemptService servico = new LoginAttemptService(props(SEGREDO, 1), relogio);

        servico.registrarFalha("ana@teste.com");
        assertThat(servico.estaBloqueado("ana@teste.com")).isTrue();

        relogio.avancarMinutos(16); // bloqueio configurado é de 15 minutos
        assertThat(servico.estaBloqueado("ana@teste.com")).isFalse();
    }

    @Test
    void login_sucessoZeraAsFalhas() {
        LoginAttemptService servico = new LoginAttemptService(props(SEGREDO, 2), Clock.systemUTC());
        servico.registrarFalha("ana@teste.com");
        servico.registrarSucesso("ana@teste.com");
        servico.registrarFalha("ana@teste.com");
        assertThat(servico.estaBloqueado("ana@teste.com")).isFalse();
    }

    @Test
    void login_falhasAntigasForaDaJanelaNaoSomam() {
        RelogioAjustavel relogio = new RelogioAjustavel(Instant.parse("2026-10-07T15:00:00Z"));
        LoginAttemptService servico = new LoginAttemptService(props(SEGREDO, 2), relogio);

        servico.registrarFalha("ana@teste.com");
        relogio.avancarMinutos(20); // janela de 15 min passou
        servico.registrarFalha("ana@teste.com");
        assertThat(servico.estaBloqueado("ana@teste.com")).isFalse();
    }

    @Test
    void jwt_tokenDeixaDeValerQuandoASenhaMuda() {
        JwtService jwt = new JwtService(props(SEGREDO, 5));
        Usuario u = usuario();
        u.setSenhaHash("$2a$10$hashAntigo");
        String token = jwt.gerarToken(u);
        String versaoNoToken = jwt.validar(token).orElseThrow().versaoSenha();
        assertThat(versaoNoToken).isEqualTo(jwt.versaoSenha(u));

        u.setSenhaHash("$2a$10$hashNovo");
        assertThat(versaoNoToken).isNotEqualTo(jwt.versaoSenha(u));
    }

    /** Relógio que o teste consegue adiantar. */
    private static final class RelogioAjustavel extends Clock {
        private Instant agora;

        RelogioAjustavel(Instant inicio) {
            this.agora = inicio;
        }

        void avancarMinutos(long minutos) {
            agora = agora.plusSeconds(minutos * 60);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return agora;
        }
    }
}
