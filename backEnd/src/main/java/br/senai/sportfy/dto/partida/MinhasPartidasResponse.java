package br.senai.sportfy.dto.partida;

import java.util.List;

/** Partidas que o usuário criou e partidas de outras pessoas em que está confirmado. */
public record MinhasPartidasResponse(List<PartidaResponse> criadas, List<PartidaResponse> participando) {
}
