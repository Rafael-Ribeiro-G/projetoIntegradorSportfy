package br.senai.sportfy.security;

import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import br.senai.sportfy.domain.enums.Perfil;

/**
 * Usuário logado na requisição atual.
 * Nos controllers: {@code @AuthenticationPrincipal UsuarioAutenticado usuario}
 */
public record UsuarioAutenticado(Long id, String email, Perfil perfil) {

    public boolean isAdmin() {
        return perfil == Perfil.ADMIN;
    }

    public List<GrantedAuthority> authorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + perfil.name()));
    }
}
