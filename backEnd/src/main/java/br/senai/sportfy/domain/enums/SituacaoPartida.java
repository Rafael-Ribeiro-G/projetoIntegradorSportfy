package br.senai.sportfy.domain.enums;

/**
 * Situação calculada da partida, devolvida para o front/mobile.
 * Não é gravada no banco: depende da hora atual e do número de confirmados.
 */
public enum SituacaoPartida {
    ABERTA,
    LOTADA,
    EM_ANDAMENTO,
    ENCERRADA,
    CANCELADA
}
