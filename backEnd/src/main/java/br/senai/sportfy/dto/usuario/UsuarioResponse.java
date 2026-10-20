package br.senai.sportfy.dto.usuario;

import java.time.LocalDateTime;
import java.util.UUID;

import br.senai.sportfy.domain.Usuario;

/** Dados completos do próprio usuário (tela Conta/Perfil). */
public record UsuarioResponse(
        UUID id,
        String nome,
        String email,
        String telefone,
        Integer nivelHabilidade,
        LocalDateTime criadoEm) {

    public static UsuarioResponse de(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getNome(), u.getEmail(), u.getTelefone(), u.getNivelHabilidade(),
                u.getCriadoEm());
    }
}
