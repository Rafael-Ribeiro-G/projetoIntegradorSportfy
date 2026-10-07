package br.senai.sportfy.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.senai.sportfy.domain.Esporte;

public interface EsporteRepository extends JpaRepository<Esporte, Long> {

    List<Esporte> findByAtivoTrueOrderByNomeAsc();

    List<Esporte> findAllByOrderByNomeAsc();

    boolean existsByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);
}
