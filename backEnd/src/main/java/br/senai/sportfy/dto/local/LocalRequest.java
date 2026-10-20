package br.senai.sportfy.dto.local;

import static br.senai.sportfy.dto.Validacoes.MSG_TEXTO_SEGURO;
import static br.senai.sportfy.dto.Validacoes.TEXTO_SEGURO;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Cadastro/edição de local. Latitude e longitude são opcionais, mas vão juntas. */
public record LocalRequest(

        @NotBlank(message = "é obrigatório")
        @Size(max = 150, message = "deve ter no máximo 150 caracteres")
        @Pattern(regexp = TEXTO_SEGURO, message = MSG_TEXTO_SEGURO)
        String nome,

        @NotBlank(message = "é obrigatório")
        @Size(max = 300, message = "deve ter no máximo 300 caracteres")
        @Pattern(regexp = TEXTO_SEGURO, message = MSG_TEXTO_SEGURO)
        String endereco,

        @DecimalMin(value = "-90.0", message = "latitude inválida")
        @DecimalMax(value = "90.0", message = "latitude inválida")
        Double latitude,

        @DecimalMin(value = "-180.0", message = "longitude inválida")
        @DecimalMax(value = "180.0", message = "longitude inválida")
        Double longitude) {
}
