package br.senai.sportfy.dto.usuario;

import java.util.UUID;

import br.senai.sportfy.domain.Usuario;

/** Só id e nome (criador da partida, dono do local). */
public record UsuarioResumo(UUID id, String nome) {

    public static UsuarioResumo de(Usuario u) {
        return u == null ? null : new UsuarioResumo(u.getId(), u.getNome());
    }
}
