package br.senai.sportfy.dto.partida;

import java.time.LocalDateTime;
import java.util.UUID;

import br.senai.sportfy.domain.enums.SituacaoPartida;
import br.senai.sportfy.domain.enums.StatusPartida;
import br.senai.sportfy.dto.esporte.EsporteResponse;
import br.senai.sportfy.dto.local.LocalResumo;
import br.senai.sportfy.dto.usuario.UsuarioResumo;

/**
 * Partida como o front/mobile recebe.
 * - vagasRestantes: vazio quando a partida não tem limite de jogadores;
 * - situacao: calculada (ABERTA, LOTADA, EM_ANDAMENTO, ENCERRADA, CANCELADA);
 * - distanciaKm: só quando a busca informa latitude/longitude;
 * - usuarioParticipa: só quando há usuário logado.
 */
public record PartidaResponse(
        UUID id,
        EsporteResponse esporte,
        QuadraInfo quadra,
        LocalResumo local,
        UsuarioResumo criador,
        LocalDateTime dataHoraInicio,
        LocalDateTime dataHoraFim,
        long duracaoMinutos,
        Integer maxJogadores,
        long participantesConfirmados,
        Long vagasRestantes,
        StatusPartida status,
        SituacaoPartida situacao,
        Double distanciaKm,
        Boolean usuarioParticipa) {

    public record QuadraInfo(UUID id, String nome, java.math.BigDecimal precoHora) {
    }
}
