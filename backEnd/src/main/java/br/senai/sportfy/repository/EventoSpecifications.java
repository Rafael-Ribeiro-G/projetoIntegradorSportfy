package br.senai.sportfy.repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import br.senai.sportfy.domain.Evento;
import br.senai.sportfy.domain.enums.StatusEvento;
import br.senai.sportfy.dto.evento.EventoFiltro;
import br.senai.sportfy.util.GeoUtils;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

/**
 * Monta a consulta dinâmica da busca de eventos a partir dos filtros opcionais.
 * Usa a Criteria API (consultas parametrizadas), então não há risco de SQL Injection.
 */
public final class EventoSpecifications {

    private static final char ESCAPE = '\\';

    private EventoSpecifications() {
    }

    public static Specification<Evento> comFiltro(EventoFiltro f, LocalDateTime agora) {
        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();

            if (!f.incluirCancelados()) {
                predicados.add(cb.equal(root.get("status"), StatusEvento.ATIVO));
            }
            if (!f.incluirEncerrados()) {
                predicados.add(cb.greaterThan(root.<LocalDateTime>get("dataHoraInicio"), agora));
            }
            if (f.esporteId() != null) {
                predicados.add(cb.equal(root.get("esporte").get("id"), f.esporteId()));
            }
            if (temTexto(f.cidade())) {
                predicados.add(contem(cb, root.<String>get("cidade"), f.cidade()));
            }
            if (temTexto(f.texto())) {
                predicados.add(cb.or(
                        contem(cb, root.<String>get("titulo"), f.texto()),
                        contem(cb, root.<String>get("descricao"), f.texto()),
                        contem(cb, root.<String>get("localNome"), f.texto())));
            }
            if (f.nivel() != null) {
                predicados.add(cb.equal(root.get("nivel"), f.nivel()));
            }
            if (f.dataInicio() != null) {
                predicados.add(cb.greaterThanOrEqualTo(root.<LocalDateTime>get("dataHoraInicio"), f.dataInicio()));
            }
            if (f.dataFim() != null) {
                predicados.add(cb.lessThanOrEqualTo(root.<LocalDateTime>get("dataHoraInicio"), f.dataFim()));
            }
            if (f.temLocalizacao() && f.raioKm() != null) {
                double[] caixa = GeoUtils.caixaDelimitadora(f.latitude(), f.longitude(), f.raioKm());
                predicados.add(cb.between(root.<Double>get("latitude"), caixa[0], caixa[1]));
                predicados.add(cb.between(root.<Double>get("longitude"), caixa[2], caixa[3]));
            }

            return cb.and(predicados.toArray(new Predicate[0]));
        };
    }

    private static boolean temTexto(String s) {
        return s != null && !s.isBlank();
    }

    /** LIKE '%texto%' sem diferenciar maiúsculas, escapando os curingas % e _ digitados pelo usuário. */
    private static Predicate contem(CriteriaBuilder cb, Expression<String> campo, String texto) {
        String escapado = texto.trim().toLowerCase()
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return cb.like(cb.lower(campo), "%" + escapado + "%", ESCAPE);
    }
}
