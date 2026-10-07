package br.senai.sportfy.dto.evento;

import static br.senai.sportfy.dto.Validacoes.MSG_TEXTO_SEGURO;
import static br.senai.sportfy.dto.Validacoes.TEXTO_SEGURO;

import java.time.LocalDateTime;

import br.senai.sportfy.domain.enums.NivelHabilidade;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Dados para criar ou editar um evento (tela Criar Evento).
 * dataHoraInicio no formato ISO: "2026-10-20T19:30:00" (horário de Brasília).
 */
public record EventoRequest(

        @NotBlank(message = "é obrigatório")
        @Size(min = 3, max = 100, message = "deve ter entre 3 e 100 caracteres")
        @Pattern(regexp = TEXTO_SEGURO, message = MSG_TEXTO_SEGURO)
        String titulo,

        @Size(max = 1000, message = "deve ter no máximo 1000 caracteres")
        @Pattern(regexp = TEXTO_SEGURO, message = MSG_TEXTO_SEGURO)
        String descricao,

        @NotNull(message = "é obrigatório")
        Long esporteId,

        @NotNull(message = "é obrigatória")
        @Future(message = "deve ser uma data futura")
        LocalDateTime dataHoraInicio,

        @NotNull(message = "é obrigatória")
        @Min(value = 15, message = "deve ser no mínimo 15 minutos")
        @Max(value = 720, message = "deve ser no máximo 720 minutos (12h)")
        Integer duracaoMinutos,

        @NotBlank(message = "é obrigatório")
        @Size(max = 150, message = "deve ter no máximo 150 caracteres")
        @Pattern(regexp = TEXTO_SEGURO, message = MSG_TEXTO_SEGURO)
        String localNome,

        @Size(max = 255, message = "deve ter no máximo 255 caracteres")
        @Pattern(regexp = TEXTO_SEGURO, message = MSG_TEXTO_SEGURO)
        String endereco,

        @NotBlank(message = "é obrigatória")
        @Size(max = 100, message = "deve ter no máximo 100 caracteres")
        @Pattern(regexp = TEXTO_SEGURO, message = MSG_TEXTO_SEGURO)
        String cidade,

        @DecimalMin(value = "-90.0", message = "latitude inválida")
        @DecimalMax(value = "90.0", message = "latitude inválida")
        Double latitude,

        @DecimalMin(value = "-180.0", message = "longitude inválida")
        @DecimalMax(value = "180.0", message = "longitude inválida")
        Double longitude,

        @NotNull(message = "é obrigatório")
        @Min(value = 2, message = "deve ser no mínimo 2")
        @Max(value = 100, message = "deve ser no máximo 100")
        Integer maxParticipantes,

        NivelHabilidade nivel) {
}
