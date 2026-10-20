package br.senai.sportfy.dto.partida;

import java.util.UUID;

/** Participante confirmado de uma partida (sem e-mail/telefone). */
public record ParticipanteResponse(UUID id, String nome, Integer nivelHabilidade, boolean criador) {
}
