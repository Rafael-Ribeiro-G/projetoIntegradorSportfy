package br.senai.sportfy.dto.esporte;

import static br.senai.sportfy.dto.Validacoes.MSG_TEXTO_SEGURO;
import static br.senai.sportfy.dto.Validacoes.TEXTO_SEGURO;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EsporteRequest(

        @NotBlank(message = "é obrigatório")
        @Size(min = 2, max = 60, message = "deve ter entre 2 e 60 caracteres")
        @Pattern(regexp = TEXTO_SEGURO, message = MSG_TEXTO_SEGURO)
        String nome,

        @Size(max = 255, message = "deve ter no máximo 255 caracteres")
        @Pattern(regexp = TEXTO_SEGURO, message = MSG_TEXTO_SEGURO)
        String descricao,

        @Min(value = 1, message = "deve ser no mínimo 1")
        @Max(value = 100, message = "deve ser no máximo 100")
        Integer jogadoresSugeridos,

        Boolean ativo) {
}
