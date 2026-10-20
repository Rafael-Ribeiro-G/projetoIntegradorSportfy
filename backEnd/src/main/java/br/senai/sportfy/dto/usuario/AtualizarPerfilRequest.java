package br.senai.sportfy.dto.usuario;

import static br.senai.sportfy.dto.Validacoes.MSG_TELEFONE;
import static br.senai.sportfy.dto.Validacoes.MSG_TEXTO_SEGURO;
import static br.senai.sportfy.dto.Validacoes.MSG_URL_IMAGEM;
import static br.senai.sportfy.dto.Validacoes.TELEFONE;
import static br.senai.sportfy.dto.Validacoes.TEXTO_SEGURO;
import static br.senai.sportfy.dto.Validacoes.URL_IMAGEM;

import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Atualização do perfil (PUT): os campos enviados substituem os atuais.
 * Telefone, nível, foto e bio enviados como null são apagados.
 * esportesFavoritosIds: a lista nova substitui a atual; se não vier (null), os favoritos ficam como estão.
 */
public record AtualizarPerfilRequest(

        @NotBlank(message = "é obrigatório")
        @Size(min = 2, max = 100, message = "deve ter entre 2 e 100 caracteres")
        @Pattern(regexp = TEXTO_SEGURO, message = MSG_TEXTO_SEGURO)
        String nome,

        @Pattern(regexp = TELEFONE, message = MSG_TELEFONE)
        String telefone,

        @Min(value = 1, message = "deve ser de 1 a 5")
        @Max(value = 5, message = "deve ser de 1 a 5")
        Integer nivelHabilidade,

        @Pattern(regexp = URL_IMAGEM, message = MSG_URL_IMAGEM)
        String fotoUrl,

        @Size(max = 500, message = "deve ter no máximo 500 caracteres")
        @Pattern(regexp = TEXTO_SEGURO, message = MSG_TEXTO_SEGURO)
        String bio,

        @Size(max = 20, message = "no máximo 20 esportes favoritos")
        List<@NotNull Integer> esportesFavoritosIds) {
}
