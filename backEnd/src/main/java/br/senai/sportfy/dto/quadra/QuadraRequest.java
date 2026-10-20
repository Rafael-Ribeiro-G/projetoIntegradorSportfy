package br.senai.sportfy.dto.quadra;

import static br.senai.sportfy.dto.Validacoes.MSG_TEXTO_SEGURO;
import static br.senai.sportfy.dto.Validacoes.TEXTO_SEGURO;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Cadastro/edição de quadra. localId é obrigatório no cadastro; na edição é ignorado
 * (a quadra não muda de local). precoHora é opcional (vazio = gratuita/não informado).
 */
public record QuadraRequest(

        UUID localId,

        @NotNull(message = "é obrigatório")
        Integer esporteId,

        @NotBlank(message = "é obrigatório")
        @Size(max = 150, message = "deve ter no máximo 150 caracteres")
        @Pattern(regexp = TEXTO_SEGURO, message = MSG_TEXTO_SEGURO)
        String nome,

        @DecimalMin(value = "0.00", message = "não pode ser negativo")
        @DecimalMax(value = "99999999.99", message = "valor muito alto")
        @Digits(integer = 8, fraction = 2, message = "use no máximo 2 casas decimais")
        BigDecimal precoHora) {
}
