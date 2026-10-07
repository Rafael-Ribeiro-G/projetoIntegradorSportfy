package br.senai.sportfy.dto.esporte;

import br.senai.sportfy.domain.Esporte;

public record EsporteResponse(Long id, String nome, String descricao, Integer jogadoresSugeridos, boolean ativo) {

    public static EsporteResponse de(Esporte e) {
        return new EsporteResponse(e.getId(), e.getNome(), e.getDescricao(), e.getJogadoresSugeridos(), e.isAtivo());
    }
}
