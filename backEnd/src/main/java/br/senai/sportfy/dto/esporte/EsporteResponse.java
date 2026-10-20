package br.senai.sportfy.dto.esporte;

import br.senai.sportfy.domain.Esporte;

public record EsporteResponse(Integer id, String nome, Integer quantidadeJogadoresPadrao) {

    public static EsporteResponse de(Esporte e) {
        return new EsporteResponse(e.getId(), e.getNome(), e.getQuantidadeJogadoresPadrao());
    }
}
