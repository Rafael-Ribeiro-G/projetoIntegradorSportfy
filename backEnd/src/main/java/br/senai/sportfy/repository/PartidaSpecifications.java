package br.senai.sportfy.repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import br.senai.sportfy.domain.Esporte;
import br.senai.sportfy.domain.Local;
import br.senai.sportfy.domain.Partida;
import br.senai.sportfy.domain.PartidaParticipante;
import br.senai.sportfy.domain.Quadra;
import br.senai.sportfy.domain.enums.StatusConfirmacao;
import br.senai.sportfy.domain.enums.StatusPartida;
import br.senai.sportfy.dto.partida.PartidaFiltro;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

/** Monta o WHERE da busca de partidas a partir dos filtros opcionais. */
public final class PartidaSpecifications {

    private PartidaSpecifications() {
    }

    /**
     * @param usuarioLogadoId quem está buscando (null = visitante). Partidas em locais privados
     *                        só aparecem para quem criou ou está confirmado nelas.
     */
    public static Specification<Partida> comFiltro(PartidaFiltro f, LocalDateTime agora, UUID usuarioLogadoId) {
        return (root, query, cb) -> {
            List<Predicate> regras = new ArrayList<>();
            Join<Partida, Local> local = root.join("local");
            Join<Partida, Esporte> esporte = root.join("esporte");
            Join<Partida, Quadra> quadra = root.join("quadra", JoinType.LEFT);

            if (usuarioLogadoId == null) {
                regras.add(cb.isTrue(local.get("publico")));
            } else {
                Subquery<Integer> participa = query.subquery(Integer.class);
                Root<PartidaParticipante> pp = participa.from(PartidaParticipante.class);
                participa.select(cb.literal(1)).where(
                        cb.equal(pp.get("partida"), root),
                        cb.equal(pp.get("usuario").get("id"), usuarioLogadoId),
                        cb.equal(pp.get("statusConfirmacao"), StatusConfirmacao.CONFIRMADO));
                regras.add(cb.or(
                        cb.isTrue(local.get("publico")),
                        cb.equal(root.get("criador").get("id"), usuarioLogadoId),
                        cb.exists(participa)));
            }
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
            if (f.tipoLocal() != null) {
                regras.add(cb.equal(local.get("tipo"), f.tipoLocal()));
            }
            if (f.texto() != null && !f.texto().isBlank()) {
                String padrao = "%" + f.texto().trim().toLowerCase() + "%";
                regras.add(cb.or(
                        cb.like(cb.lower(root.get("titulo")), padrao),
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
