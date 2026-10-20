package br.senai.sportfy.dto.partida;

import java.util.UUID;

/** Participante confirmado de uma partida (sem e-mail/telefone). fotoUrl vazio = sem foto. */
public record ParticipanteResponse(UUID id, String nome, String fotoUrl, Integer nivelHabilidade, boolean criador) {
}
