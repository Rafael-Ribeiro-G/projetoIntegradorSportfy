package br.senai.sportfy.dto.esporte;

import br.senai.sportfy.domain.Esporte;

public record EsporteResumo(Long id, String nome) {

    public static EsporteResumo de(Esporte esporte) {
        return new EsporteResumo(esporte.getId(), esporte.getNome());
    }
}
