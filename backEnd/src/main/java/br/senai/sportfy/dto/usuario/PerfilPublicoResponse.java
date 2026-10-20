package br.senai.sportfy.dto.usuario;

import java.util.UUID;

import br.senai.sportfy.domain.Usuario;

/** Perfil visto por OUTROS usuários. LGPD: não expõe e-mail nem telefone. */
public record PerfilPublicoResponse(UUID id, String nome, Integer nivelHabilidade) {

    public static PerfilPublicoResponse de(Usuario u) {
        return new PerfilPublicoResponse(u.getId(), u.getNome(), u.getNivelHabilidade());
    }
}
