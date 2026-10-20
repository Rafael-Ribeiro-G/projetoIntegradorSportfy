package br.senai.sportfy.dto.quadra;

import java.math.BigDecimal;
import java.util.UUID;

import br.senai.sportfy.domain.Quadra;
import br.senai.sportfy.dto.esporte.EsporteResponse;
import br.senai.sportfy.dto.local.LocalResumo;

public record QuadraResponse(UUID id, String nome, BigDecimal precoHora, EsporteResponse esporte, LocalResumo local) {

    public static QuadraResponse de(Quadra q) {
        return new QuadraResponse(q.getId(), q.getNome(), q.getPrecoHora(), EsporteResponse.de(q.getEsporte()),
                LocalResumo.de(q.getLocal()));
    }
}
