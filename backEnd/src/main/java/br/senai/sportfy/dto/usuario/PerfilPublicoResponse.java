package br.senai.sportfy.dto.usuario;

import java.util.Comparator;
import java.util.List;

import br.senai.sportfy.domain.Usuario;
import br.senai.sportfy.domain.enums.NivelHabilidade;
import br.senai.sportfy.dto.esporte.EsporteResumo;

/**
 * Perfil visto por OUTROS usuários.
 * LGPD: não expõe e-mail, telefone, data de nascimento nem localização exata.
 */
public record PerfilPublicoResponse(
        Long id,
        String nome,
        String cidade,
        String bio,
        NivelHabilidade nivel,
        List<EsporteResumo> esportesFavoritos) {

    public static PerfilPublicoResponse de(Usuario u) {
        List<EsporteResumo> esportes = u.getEsportesFavoritos().stream()
                .map(EsporteResumo::de)
                .sorted(Comparator.comparing(EsporteResumo::nome))
                .toList();
        return new PerfilPublicoResponse(u.getId(), u.getNome(), u.getCidade(), u.getBio(), u.getNivel(), esportes);
    }
}
