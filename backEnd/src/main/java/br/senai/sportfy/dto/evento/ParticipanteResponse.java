package br.senai.sportfy.dto.evento;

import java.time.LocalDateTime;

import br.senai.sportfy.domain.enums.NivelHabilidade;

public record ParticipanteResponse(
        Long usuarioId,
        String nome,
        NivelHabilidade nivel,
        boolean organizador,
        LocalDateTime confirmadoEm) {
}
