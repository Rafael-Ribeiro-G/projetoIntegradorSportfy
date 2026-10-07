package br.senai.sportfy.domain.enums;

/**
 * Situação calculada do evento, devolvida para o front/mobile.
 * Não é gravada no banco: depende da data atual e do número de participantes.
 */
public enum SituacaoEvento {
    ABERTO,
    LOTADO,
    EM_ANDAMENTO,
    ENCERRADO,
    CANCELADO
}
