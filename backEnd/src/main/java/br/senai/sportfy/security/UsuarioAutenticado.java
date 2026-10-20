package br.senai.sportfy.security;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Usuário logado na requisição atual.
 * Nos controllers: {@code @AuthenticationPrincipal UsuarioAutenticado usuario}
 */
public record UsuarioAutenticado(UUID id, String email) {

    public List<GrantedAuthority> authorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }
}
