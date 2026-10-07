package br.senai.sportfy.dto.usuario;

import static br.senai.sportfy.dto.Validacoes.MSG_SENHA_FORTE;
import static br.senai.sportfy.dto.Validacoes.SENHA_FORTE;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AlterarSenhaRequest(

        @NotBlank(message = "é obrigatória")
        @Size(max = 72)
        String senhaAtual,

        @NotBlank(message = "é obrigatória")
        @Size(min = 8, max = 72, message = "deve ter entre 8 e 72 caracteres")
        @Pattern(regexp = SENHA_FORTE, message = MSG_SENHA_FORTE)
        String novaSenha) {
}
