package br.senai.sportfy.dto.auth;

import static br.senai.sportfy.dto.Validacoes.MSG_SENHA_FORTE;
import static br.senai.sportfy.dto.Validacoes.MSG_TELEFONE;
import static br.senai.sportfy.dto.Validacoes.MSG_TEXTO_SEGURO;
import static br.senai.sportfy.dto.Validacoes.SENHA_FORTE;
import static br.senai.sportfy.dto.Validacoes.TELEFONE;
import static br.senai.sportfy.dto.Validacoes.TEXTO_SEGURO;

import java.time.LocalDate;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

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

        @Past(message = "deve ser uma data no passado")
        LocalDate dataNascimento,

        @Size(max = 100, message = "deve ter no máximo 100 caracteres")
        @Pattern(regexp = TEXTO_SEGURO, message = MSG_TEXTO_SEGURO)
        String cidade,

        /* LGPD: o usuário precisa aceitar os termos de uso e a política de privacidade. */
        @NotNull(message = "é obrigatório")
        @AssertTrue(message = "é necessário aceitar os termos de uso e a política de privacidade")
        Boolean aceiteTermos) {
}
