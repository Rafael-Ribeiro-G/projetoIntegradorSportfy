package br.senai.sportfy.dto.partida;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Criar/editar partida (tela Criar Partida).
 * Datas no formato 2026-10-25T19:00:00 (horário de Brasília).
 * maxJogadores é opcional: se vier vazio, usa a quantidade padrão do esporte da quadra.
 */
public record PartidaRequest(

        @NotNull(message = "é obrigatório")
        UUID quadraId,

        @NotNull(message = "é obrigatória")
        @Future(message = "deve ser no futuro")
        LocalDateTime dataHoraInicio,

        @NotNull(message = "é obrigatória")
        @Future(message = "deve ser no futuro")
        LocalDateTime dataHoraFim,

        @Min(value = 2, message = "deve ser no mínimo 2")
        @Max(value = 100, message = "deve ser no máximo 100")
        Integer maxJogadores) {
}
