package br.senai.sportfy.dto.auth;

import static br.senai.sportfy.dto.Validacoes.MSG_SENHA_FORTE;
import static br.senai.sportfy.dto.Validacoes.MSG_TELEFONE;
import static br.senai.sportfy.dto.Validacoes.MSG_TEXTO_SEGURO;
import static br.senai.sportfy.dto.Validacoes.MSG_URL_IMAGEM;
import static br.senai.sportfy.dto.Validacoes.SENHA_FORTE;
import static br.senai.sportfy.dto.Validacoes.TELEFONE;
import static br.senai.sportfy.dto.Validacoes.TEXTO_SEGURO;
import static br.senai.sportfy.dto.Validacoes.URL_IMAGEM;

import jakarta.validation.constraints.Email;
import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Tela Criar Conta. Só nome, e-mail e senha são obrigatórios; nível de habilidade vai de 1 a 5. */
public record RegistroRequest(

        @NotBlank(message = "é obrigatório")
        @Size(min = 2, max = 100, message = "deve ter entre 2 e 100 caracteres")
        @Pattern(regexp = TEXTO_SEGURO, message = MSG_TEXTO_SEGURO)
        String nome,

        @NotBlank(message = "é obrigatório")
        @Email(message = "deve ser um e-mail válido")
        @Size(max = 150, message = "deve ter no máximo 150 caracteres")
        String email,

        @NotBlank(message = "é obrigatória")
        @Size(min = 8, max = 72, message = "deve ter entre 8 e 72 caracteres")
        @Pattern(regexp = SENHA_FORTE, message = MSG_SENHA_FORTE)
        String senha,

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
