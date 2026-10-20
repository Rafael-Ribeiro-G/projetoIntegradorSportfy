package br.senai.sportfy.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.senai.sportfy.domain.Esporte;

public interface EsporteRepository extends JpaRepository<Esporte, Integer> {

    List<Esporte> findAllByOrderByNomeAsc();
}
