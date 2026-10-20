package br.senai.sportfy.dto.local;

import java.util.List;
import java.util.UUID;

import br.senai.sportfy.domain.Local;
import br.senai.sportfy.dto.quadra.QuadraResumo;
import br.senai.sportfy.dto.usuario.UsuarioResumo;

/**
 * Local com coordenadas separadas. "quadras" só vem no detalhe (GET /api/locais/{id});
 * "distanciaKm" só vem quando a busca informa latitude/longitude.
 */
public record LocalResponse(
        UUID id,
        String nome,
        String endereco,
        Double latitude,
        Double longitude,
        UsuarioResumo dono,
        Double distanciaKm,
        List<QuadraResumo> quadras) {

    public static LocalResponse de(Local l, Double distanciaKm, List<QuadraResumo> quadras) {
        return new LocalResponse(l.getId(), l.getNome(), l.getEndereco(), l.getLatitude(), l.getLongitude(),
                UsuarioResumo.de(l.getDono()), distanciaKm, quadras);
    }
}
