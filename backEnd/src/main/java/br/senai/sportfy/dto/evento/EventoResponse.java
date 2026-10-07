package br.senai.sportfy.dto.evento;

import java.time.LocalDateTime;

import br.senai.sportfy.domain.enums.NivelHabilidade;
import br.senai.sportfy.domain.enums.SituacaoEvento;
import br.senai.sportfy.dto.esporte.EsporteResumo;

/**
 * Evento como é devolvido para as telas Home / Ver Eventos.
 * distanciaKm só vem preenchida quando a busca informa latitude/longitude.
 * usuarioParticipa só vem preenchido quando o usuário está logado.
 */
public record EventoResponse(
        Long id,
        String titulo,
        String descricao,
        EsporteResumo esporte,
        OrganizadorResumo organizador,
        LocalDateTime dataHoraInicio,
        LocalDateTime dataHoraFim,
        Integer duracaoMinutos,
        String localNome,
        String endereco,
        String cidade,
        Double latitude,
        Double longitude,
        Integer maxParticipantes,
        long participantesConfirmados,
        long vagasRestantes,
        NivelHabilidade nivel,
        SituacaoEvento situacao,
        Double distanciaKm,
        Boolean usuarioParticipa,
        LocalDateTime criadoEm) {

    public record OrganizadorResumo(Long id, String nome) {
    }
}
