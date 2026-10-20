package br.senai.sportfy.dto.usuario;

import java.util.List;
import java.util.UUID;

import br.senai.sportfy.domain.Usuario;
import br.senai.sportfy.dto.esporte.EsporteResponse;

/** Perfil visto por OUTROS usuários. LGPD: não expõe e-mail nem telefone. */
public record PerfilPublicoResponse(UUID id, String nome, Integer nivelHabilidade, String fotoUrl, String bio,
                                    List<EsporteResponse> esportesFavoritos) {

    public static PerfilPublicoResponse de(Usuario u) {
        return new PerfilPublicoResponse(u.getId(), u.getNome(), u.getNivelHabilidade(), u.getFotoUrl(), u.getBio(),
                UsuarioResponse.favoritos(u));
    }
}
