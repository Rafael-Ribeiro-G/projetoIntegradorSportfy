package br.senai.sportfy.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.senai.sportfy.domain.Participacao;
import br.senai.sportfy.domain.enums.StatusParticipacao;

public interface ParticipacaoRepository extends JpaRepository<Participacao, Long> {

    Optional<Participacao> findByEventoIdAndUsuarioId(Long eventoId, Long usuarioId);

    long countByEventoIdAndStatus(Long eventoId, StatusParticipacao status);

    boolean existsByEventoIdAndUsuarioIdAndStatus(Long eventoId, Long usuarioId, StatusParticipacao status);

    /** Total de confirmados por evento, em uma única consulta (para listagens). */
    @Query("""
            select p.evento.id, count(p) from Participacao p
            where p.evento.id in :eventoIds and p.status = :status
            group by p.evento.id
            """)
    List<Object[]> contarPorEventos(@Param("eventoIds") Collection<Long> eventoIds,
                                    @Param("status") StatusParticipacao status);

    @Query("""
            select p from Participacao p
            join fetch p.usuario
            where p.evento.id = :eventoId and p.status = :status
            order by p.criadoEm asc
            """)
    List<Participacao> buscarPorEvento(@Param("eventoId") Long eventoId,
                                       @Param("status") StatusParticipacao status);

    @Query("""
            select p from Participacao p
            join fetch p.evento e
            join fetch e.esporte
            join fetch e.organizador
            where p.usuario.id = :usuarioId and p.status = :status
            order by e.dataHoraInicio desc
            """)
    List<Participacao> buscarPorUsuario(@Param("usuarioId") Long usuarioId,
                                        @Param("status") StatusParticipacao status);

    @Query("select p.evento.id from Participacao p where p.usuario.id = :usuarioId and p.status = :status")
    List<Long> buscarIdsEventosDoUsuario(@Param("usuarioId") Long usuarioId,
                                         @Param("status") StatusParticipacao status);

    List<Participacao> findByUsuarioIdAndStatus(Long usuarioId, StatusParticipacao status);
}
