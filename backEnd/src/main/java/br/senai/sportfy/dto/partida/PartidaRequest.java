package br.senai.sportfy.dto.partida;

import static br.senai.sportfy.dto.Validacoes.MSG_TEXTO_SEGURO;
import static br.senai.sportfy.dto.Validacoes.MSG_URL_IMAGEM;
import static br.senai.sportfy.dto.Validacoes.TEXTO_SEGURO;
import static br.senai.sportfy.dto.Validacoes.URL_IMAGEM;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Criar/editar partida (tela Criar Partida). Dois jeitos:
 * - numa QUADRA: envie quadraId (o local e o esporte vêm dela);
 * - sem quadra (corrida no parque, treino na academia...): envie localId + esporteId.
 * Datas no formato 2026-10-25T19:00:00 (horário de Brasília).
 * maxJogadores é opcional: sem ele, usa a quantidade padrão do esporte (que pode ser "sem limite").
 */
public record PartidaRequest(

        UUID localId,

        UUID quadraId,

        Integer esporteId,

        @Size(max = 100, message = "deve ter no máximo 100 caracteres")
        @Pattern(regexp = TEXTO_SEGURO, message = MSG_TEXTO_SEGURO)
        String titulo,

        @Size(max = 1000, message = "deve ter no máximo 1000 caracteres")
        @Pattern(regexp = TEXTO_SEGURO, message = MSG_TEXTO_SEGURO)
        String descricao,

        @Pattern(regexp = URL_IMAGEM, message = MSG_URL_IMAGEM)
        String imagemUrl,

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
