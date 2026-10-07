package br.senai.sportfy.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import br.senai.sportfy.config.SportfyProperties;

/**
 * Proteção contra força bruta: depois de N senhas erradas para o mesmo e-mail dentro
 * da janela de tempo, o login fica bloqueado por alguns minutos.
 *
 * Fica em memória (reinicia junto com a API), o que é suficiente para uma única instância.
 * Registros vencidos são descartados para o mapa não crescer sem limite.
 */
@Service
public class LoginAttemptService {

    /** Acima disso, faz uma limpeza dos registros vencidos antes de inserir. */
    private static final int LIMITE_REGISTROS = 10_000;

    private final Map<String, Tentativas> tentativas = new ConcurrentHashMap<>();
    private final int maxTentativas;
    private final Duration janela;
    private final Clock clock;

    public LoginAttemptService(SportfyProperties properties, Clock clock) {
        this.maxTentativas = Math.max(1, properties.login().maxTentativas());
        // a mesma duração serve como janela de contagem e como tempo de bloqueio
        this.janela = Duration.ofMinutes(Math.max(1, properties.login().bloqueioMinutos()));
        this.clock = clock;
    }

    public boolean estaBloqueado(String email) {
        String chave = chave(email);
        Tentativas t = tentativas.get(chave);
        if (t == null) {
            return false;
        }
        Instant agora = Instant.now(clock);
        if (t.venceuEm(agora, janela)) {
            tentativas.remove(chave, t);
            return false;
        }
        return t.bloqueadoAte() != null && agora.isBefore(t.bloqueadoAte());
    }

    public void registrarFalha(String email) {
        Instant agora = Instant.now(clock);
        if (tentativas.size() >= LIMITE_REGISTROS) {
            tentativas.entrySet().removeIf(e -> e.getValue().venceuEm(agora, janela));
        }
        tentativas.compute(chave(email), (k, atual) -> {
            Tentativas base = (atual == null || atual.venceuEm(agora, janela)) ? new Tentativas(0, agora, null) : atual;
            int quantidade = base.quantidade() + 1;
            Instant bloqueadoAte = quantidade >= maxTentativas ? agora.plus(janela) : null;
            return new Tentativas(quantidade, base.primeiraFalha(), bloqueadoAte);
        });
    }

    public void registrarSucesso(String email) {
        tentativas.remove(chave(email));
    }

    public long getMinutosBloqueio() {
        return janela.toMinutes();
    }

    int totalRegistros() {
        return tentativas.size();
    }

    private String chave(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private record Tentativas(int quantidade, Instant primeiraFalha, Instant bloqueadoAte) {

        /** Vencido = bloqueio já terminou, ou (sem bloqueio) a janela de contagem passou. */
        boolean venceuEm(Instant agora, Duration janela) {
            if (bloqueadoAte != null) {
                return !agora.isBefore(bloqueadoAte);
            }
            return !agora.isBefore(primeiraFalha.plus(janela));
        }
    }
}
