package br.senai.sportfy.domain.enums;

/**
 * Coluna partida_participantes.status_confirmacao ("pendente" é o padrão da tabela).
 * A API grava "confirmado" quando a pessoa entra na partida; só os confirmados ocupam vaga.
 */
public enum StatusConfirmacao {
    PENDENTE,
    CONFIRMADO;

    public String valorNoBanco() {
        return name().toLowerCase();
    }

    public static StatusConfirmacao doBanco(String valor) {
        return valor != null && valor.trim().equalsIgnoreCase("confirmado") ? CONFIRMADO : PENDENTE;
    }
}
