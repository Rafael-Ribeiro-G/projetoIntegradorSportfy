package br.senai.sportfy.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.senai.sportfy.domain.Quadra;

public interface QuadraRepository extends JpaRepository<Quadra, UUID> {

    @Query("select q from Quadra q join fetch q.local l join fetch q.esporte order by l.nome asc, q.nome asc")
    List<Quadra> buscarTodas();

    @Query("select q from Quadra q join fetch q.local l join fetch q.esporte where l.id = :localId order by q.nome asc")
    List<Quadra> buscarDoLocal(@Param("localId") UUID localId);

    @Query("select q from Quadra q join fetch q.local join fetch q.esporte where q.id = :id")
    Optional<Quadra> buscarCompleta(@Param("id") UUID id);

    long countByLocalId(UUID localId);
}
