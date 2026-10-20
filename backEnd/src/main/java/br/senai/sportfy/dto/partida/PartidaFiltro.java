package br.senai.sportfy.dto.partida;

import java.time.LocalDateTime;
import java.util.UUID;

import br.senai.sportfy.domain.enums.TipoLocal;

/** Filtros opcionais da busca de partidas (GET /api/partidas). */
public record PartidaFiltro(
        Integer esporteId,
        UUID localId,
        UUID quadraId,
        TipoLocal tipoLocal,
        String texto,
        LocalDateTime dataInicio,
        LocalDateTime dataFim,
        Double latitude,
        Double longitude,
        Double raioKm,
        boolean incluirEncerradas,
        boolean incluirCanceladas) {

    public boolean temLocalizacao() {
        return latitude != null && longitude != null;
    }
}
