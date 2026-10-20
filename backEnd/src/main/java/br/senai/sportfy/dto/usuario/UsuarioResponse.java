package br.senai.sportfy.dto.usuario;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import br.senai.sportfy.domain.Usuario;
import br.senai.sportfy.dto.esporte.EsporteResponse;

/** Dados completos do próprio usuário (tela Conta/Perfil). */
public record UsuarioResponse(
        UUID id,
        String nome,
        String email,
        String telefone,
        Integer nivelHabilidade,
        String fotoUrl,
        String bio,
        List<EsporteResponse> esportesFavoritos,
        LocalDateTime criadoEm) {

    public static UsuarioResponse de(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getNome(), u.getEmail(), u.getTelefone(), u.getNivelHabilidade(),
                u.getFotoUrl(), u.getBio(), favoritos(u), u.getCriadoEm());
    }

    static List<EsporteResponse> favoritos(Usuario u) {
        return u.getEsportesFavoritos().stream()
                .map(EsporteResponse::de)
                .sorted(Comparator.comparing(EsporteResponse::nome))
                .toList();
    }
}
