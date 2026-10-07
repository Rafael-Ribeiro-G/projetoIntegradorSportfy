package br.senai.sportfy.dto.usuario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Usado para confirmar ações sensíveis, como excluir a conta. */
public record ConfirmarSenhaRequest(
        @NotBlank(message = "é obrigatória")
        @Size(max = 72)
        String senha) {
}
