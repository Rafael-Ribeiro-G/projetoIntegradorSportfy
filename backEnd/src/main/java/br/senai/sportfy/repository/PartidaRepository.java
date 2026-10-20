package br.senai.sportfy.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.Param;

import br.senai.sportfy.domain.Partida;
import br.senai.sportfy.domain.enums.StatusPartida;
import jakarta.persistence.LockModeType;

public interface PartidaRepository extends JpaRepository<Partida, UUID>, JpaSpecificationExecutor<Partida> {

    /** Já traz criador, local, esporte e quadra (evita uma consulta por partida nas listagens). */
    @Override
    @EntityGraph(attributePaths = {"criador", "local", "esporte", "quadra"})
    List<Partida> findAll(Specification<Partida> spec, Sort sort);

    @EntityGraph(attributePaths = {"criador", "local", "esporte", "quadra"})
    @Query("select p from Partida p where p.id = :id")
    Optional<Partida> buscarCompleta(@Param("id") UUID id);

    /**
     * Trava a linha da partida até o fim da transação.
     * Evita que duas pessoas ocupem a última vaga ao mesmo tempo.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Partida p where p.id = :id")
    Optional<Partida> buscarParaAtualizacao(@Param("id") UUID id);

    /** Partidas não canceladas na mesma quadra que se sobrepõem ao horário informado (exceto ignorarId). */
    @Query("""
            select count(p) from Partida p
            where p.quadra.id = :quadraId
              and (p.status is null or p.status <> :cancelada)
              and p.dataHoraInicio < :fim
              and p.dataHoraFim > :inicio
              and p.id <> :ignorarId
            """)
    long contarConflitosDeHorario(@Param("quadraId") UUID quadraId,
                                  @Param("inicio") LocalDateTime inicio,
                                  @Param("fim") LocalDateTime fim,
                                  @Param("ignorarId") UUID ignorarId,
                                  @Param("cancelada") StatusPartida cancelada);

    @EntityGraph(attributePaths = {"criador", "local", "esporte", "quadra"})
    @Query("select p from Partida p where p.criador.id = :usuarioId order by p.dataHoraInicio desc")
    List<Partida> buscarCriadasPor(@Param("usuarioId") UUID usuarioId);

    long countByQuadraId(UUID quadraId);

    long countByLocalId(UUID localId);
}
