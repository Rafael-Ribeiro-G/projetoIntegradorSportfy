package br.senai.sportfy.dto.evento;

import java.util.List;

/** Eventos do usuário logado: os que ele organiza e os que ele participa. */
public record MeusEventosResponse(
        List<EventoResponse> organizados,
        List<EventoResponse> participando) {
}
