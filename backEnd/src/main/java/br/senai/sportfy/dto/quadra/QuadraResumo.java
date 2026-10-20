package br.senai.sportfy.dto.quadra;

import java.math.BigDecimal;
import java.util.UUID;

import br.senai.sportfy.domain.Quadra;
import br.senai.sportfy.dto.esporte.EsporteResponse;

public record QuadraResumo(UUID id, String nome, BigDecimal precoHora, EsporteResponse esporte) {

    public static QuadraResumo de(Quadra q) {
        return new QuadraResumo(q.getId(), q.getNome(), q.getPrecoHora(), EsporteResponse.de(q.getEsporte()));
    }
}
