package br.senai.sportfy.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.senai.sportfy.domain.Evento;
import br.senai.sportfy.domain.enums.StatusEvento;
import jakarta.persistence.LockModeType;

public interface EventoRepository extends JpaRepository<Evento, Long>, JpaSpecificationExecutor<Evento> {

    /** Busca com esporte e organizador já carregados (evita N+1 nas listagens). */
    @Override
    @EntityGraph(attributePaths = {"esporte", "organizador"})
    Page<Evento> findAll(Specification<Evento> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"esporte", "organizador"})
    List<Evento> findAll(Specification<Evento> spec, Sort sort);

    @EntityGraph(attributePaths = {"esporte", "organizador"})
    @Query("select e from Evento e where e.id = :id")
    Optional<Evento> buscarCompleto(@Param("id") Long id);

    /**
     * Trava a linha do evento até o fim da transação.
     * Evita que duas pessoas ocupem a última vaga ao mesmo tempo.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Evento e where e.id = :id")
    Optional<Evento> buscarParaAtualizacao(@Param("id") Long id);

    @EntityGraph(attributePaths = {"esporte", "organizador"})
    @Query("""
            select e from Evento e
            where e.status = :status
              and e.dataHoraInicio > :agora
            order by e.dataHoraInicio asc
            """)
    List<Evento> buscarFuturos(@Param("status") StatusEvento status,
                               @Param("agora") LocalDateTime agora,
                               Pageable limite);

    @EntityGraph(attributePaths = {"esporte", "organizador"})
    @Query("select e from Evento e where e.organizador.id = :usuarioId order by e.dataHoraInicio desc")
    List<Evento> buscarOrganizadosPor(@Param("usuarioId") Long usuarioId);

    @Query("""
            select e from Evento e
            where e.organizador.id = :usuarioId
              and e.status = br.senai.sportfy.domain.enums.StatusEvento.ATIVO
              and e.dataHoraInicio > :agora
            """)
    List<Evento> buscarFuturosAtivosDoOrganizador(@Param("usuarioId") Long usuarioId,
                                                  @Param("agora") LocalDateTime agora);

    long countByEsporteId(Long esporteId);
}
