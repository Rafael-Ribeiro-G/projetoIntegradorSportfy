package br.senai.sportfy.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(

        @NotBlank(message = "é obrigatório")
        @Email(message = "deve ser um e-mail válido")
        @Size(max = 150)
        String email,

        @NotBlank(message = "é obrigatória")
        @Size(max = 72)
        String senha) {
}
