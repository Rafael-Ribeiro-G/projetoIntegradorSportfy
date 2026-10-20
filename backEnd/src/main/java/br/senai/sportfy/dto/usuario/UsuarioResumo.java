package br.senai.sportfy.dto.usuario;

import java.util.UUID;

import br.senai.sportfy.domain.Usuario;

/** Id, nome e foto (criador do evento, dono do local). */
public record UsuarioResumo(UUID id, String nome, String fotoUrl) {

    public static UsuarioResumo de(Usuario u) {
        return u == null ? null : new UsuarioResumo(u.getId(), u.getNome(), u.getFotoUrl());
    }
}
