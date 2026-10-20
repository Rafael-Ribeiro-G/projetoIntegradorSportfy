package br.senai.sportfy.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.senai.sportfy.domain.ParticipanteId;
import br.senai.sportfy.domain.PartidaParticipante;
import br.senai.sportfy.domain.enums.StatusConfirmacao;

public interface PartidaParticipanteRepository extends JpaRepository<PartidaParticipante, ParticipanteId> {

    @Query("""
            select count(pp) from PartidaParticipante pp
            where pp.partida.id = :partidaId and pp.statusConfirmacao = :status
            """)
    long contar(@Param("partidaId") UUID partidaId, @Param("status") StatusConfirmacao status);

    /** Total de confirmados por partida, em uma consulta só (para listagens). */
    @Query("""
            select pp.partida.id, count(pp) from PartidaParticipante pp
            where pp.partida.id in :partidaIds and pp.statusConfirmacao = :status
            group by pp.partida.id
            """)
    List<Object[]> contarPorPartidas(@Param("partidaIds") Collection<UUID> partidaIds,
                                     @Param("status") StatusConfirmacao status);

    @Query("""
            select pp from PartidaParticipante pp
            join fetch pp.usuario
            where pp.partida.id = :partidaId and pp.statusConfirmacao = :status
            order by pp.usuario.nome asc
            """)
    List<PartidaParticipante> buscarPorPartida(@Param("partidaId") UUID partidaId,
                                               @Param("status") StatusConfirmacao status);

    @Query("""
            select pp.partida.id from PartidaParticipante pp
            where pp.usuario.id = :usuarioId and pp.statusConfirmacao = :status
            """)
    List<UUID> buscarIdsPartidasDoUsuario(@Param("usuarioId") UUID usuarioId,
                                          @Param("status") StatusConfirmacao status);
}
