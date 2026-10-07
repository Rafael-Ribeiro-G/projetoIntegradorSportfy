package br.senai.sportfy.dto.evento;

import java.time.LocalDateTime;

import br.senai.sportfy.domain.enums.NivelHabilidade;

/**
 * Filtros da busca de eventos (GET /api/eventos).
 * Todos são opcionais.
 */
public record EventoFiltro(
        Long esporteId,
        String cidade,
        String texto,
        NivelHabilidade nivel,
        LocalDateTime dataInicio,
        LocalDateTime dataFim,
        Double latitude,
        Double longitude,
        Double raioKm,
        boolean incluirEncerrados,
        boolean incluirCancelados) {

    public boolean temLocalizacao() {
        return latitude != null && longitude != null;
    }
}
