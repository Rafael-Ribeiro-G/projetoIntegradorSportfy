package br.senai.sportfy.dto.usuario;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import br.senai.sportfy.domain.Usuario;
import br.senai.sportfy.domain.enums.NivelHabilidade;
import br.senai.sportfy.domain.enums.Perfil;
import br.senai.sportfy.dto.esporte.EsporteResumo;

/** Dados completos do próprio usuário (tela Conta/Perfil). */
public record UsuarioResponse(
        Long id,
        String nome,
        String email,
        String telefone,
        LocalDate dataNascimento,
        String cidade,
        String bio,
        NivelHabilidade nivel,
        Double latitude,
        Double longitude,
        Perfil perfil,
        List<EsporteResumo> esportesFavoritos,
        LocalDateTime consentimentoLgpdEm,
        LocalDateTime criadoEm) {

    public static UsuarioResponse de(Usuario u) {
        List<EsporteResumo> esportes = u.getEsportesFavoritos().stream()
                .map(EsporteResumo::de)
                .sorted(Comparator.comparing(EsporteResumo::nome))
                .toList();
        return new UsuarioResponse(u.getId(), u.getNome(), u.getEmail(), u.getTelefone(), u.getDataNascimento(),
                u.getCidade(), u.getBio(), u.getNivel(), u.getLatitude(), u.getLongitude(), u.getPerfil(),
                esportes, u.getConsentimentoLgpdEm(), u.getCriadoEm());
    }
}
