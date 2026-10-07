package br.senai.sportfy.dto.usuario;

import static br.senai.sportfy.dto.Validacoes.MSG_TELEFONE;
import static br.senai.sportfy.dto.Validacoes.MSG_TEXTO_SEGURO;
import static br.senai.sportfy.dto.Validacoes.TELEFONE;
import static br.senai.sportfy.dto.Validacoes.TEXTO_SEGURO;

import java.time.LocalDate;
import java.util.List;

import br.senai.sportfy.domain.enums.NivelHabilidade;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Atualização do perfil (PUT): os campos enviados substituem os atuais.
 * Campos opcionais enviados como null são apagados.
 */
public record AtualizarPerfilRequest(

        @NotBlank(message = "é obrigatório")
        @Size(min = 2, max = 100, message = "deve ter entre 2 e 100 caracteres")
        @Pattern(regexp = TEXTO_SEGURO, message = MSG_TEXTO_SEGURO)
        String nome,

        @Pattern(regexp = TELEFONE, message = MSG_TELEFONE)
        String telefone,

        @Past(message = "deve ser uma data no passado")
        LocalDate dataNascimento,

        @Size(max = 100, message = "deve ter no máximo 100 caracteres")
        @Pattern(regexp = TEXTO_SEGURO, message = MSG_TEXTO_SEGURO)
        String cidade,

        @Size(max = 500, message = "deve ter no máximo 500 caracteres")
        @Pattern(regexp = TEXTO_SEGURO, message = MSG_TEXTO_SEGURO)
        String bio,

        NivelHabilidade nivel,

        @DecimalMin(value = "-90.0", message = "latitude inválida")
        @DecimalMax(value = "90.0", message = "latitude inválida")
        Double latitude,

        @DecimalMin(value = "-180.0", message = "longitude inválida")
        @DecimalMax(value = "180.0", message = "longitude inválida")
        Double longitude,

        @Size(max = 20, message = "no máximo 20 esportes favoritos")
        List<@NotNull Long> esportesFavoritosIds) {
}
