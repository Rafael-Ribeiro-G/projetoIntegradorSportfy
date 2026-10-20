package br.senai.sportfy.repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import br.senai.sportfy.domain.Esporte;
import br.senai.sportfy.domain.Local;
import br.senai.sportfy.domain.Partida;
import br.senai.sportfy.domain.Quadra;
import br.senai.sportfy.domain.enums.StatusPartida;
import br.senai.sportfy.dto.partida.PartidaFiltro;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;

/** Monta o WHERE da busca de partidas a partir dos filtros opcionais. */
public final class PartidaSpecifications {

    private PartidaSpecifications() {
    }

    public static Specification<Partida> comFiltro(PartidaFiltro f, LocalDateTime agora) {
        return (root, query, cb) -> {
            List<Predicate> regras = new ArrayList<>();
            Join<Partida, Quadra> quadra = root.join("quadra");
            Join<Quadra, Local> local = quadra.join("local");
            Join<Quadra, Esporte> esporte = quadra.join("esporte");

            if (!f.incluirCanceladas()) {
                regras.add(cb.or(cb.isNull(root.get("status")),
                        cb.notEqual(root.get("status"), StatusPartida.CANCELADA)));
            }
            if (!f.incluirEncerradas()) {
                regras.add(cb.greaterThan(root.get("dataHoraFim"), agora));
            }
            if (f.esporteId() != null) {
                regras.add(cb.equal(esporte.get("id"), f.esporteId()));
            }
            if (f.localId() != null) {
                regras.add(cb.equal(local.get("id"), f.localId()));
            }
            if (f.quadraId() != null) {
                regras.add(cb.equal(quadra.get("id"), f.quadraId()));
            }
            if (f.texto() != null && !f.texto().isBlank()) {
                String padrao = "%" + f.texto().trim().toLowerCase() + "%";
                regras.add(cb.or(
                        cb.like(cb.lower(local.get("nome")), padrao),
                        cb.like(cb.lower(local.get("endereco")), padrao),
                        cb.like(cb.lower(quadra.get("nome")), padrao)));
            }
            if (f.dataInicio() != null) {
                regras.add(cb.greaterThanOrEqualTo(root.get("dataHoraInicio"), f.dataInicio()));
            }
            if (f.dataFim() != null) {
                regras.add(cb.lessThanOrEqualTo(root.get("dataHoraInicio"), f.dataFim()));
            }
            return cb.and(regras.toArray(Predicate[]::new));
        };
    }
}
