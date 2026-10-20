package br.senai.sportfy.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.senai.sportfy.dto.esporte.EsporteResponse;
import br.senai.sportfy.exception.RecursoNaoEncontradoException;
import br.senai.sportfy.repository.EsporteRepository;

/** Esportes são só leitura na API: quem cadastra é o responsável pelo banco. */
@Service
public class EsporteService {

    private final EsporteRepository esporteRepository;

    public EsporteService(EsporteRepository esporteRepository) {
        this.esporteRepository = esporteRepository;
    }

    @Transactional(readOnly = true)
    public List<EsporteResponse> listar() {
        return esporteRepository.findAllByOrderByNomeAsc().stream().map(EsporteResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public EsporteResponse buscar(Integer id) {
        return esporteRepository.findById(id)
                .map(EsporteResponse::de)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Esporte não encontrado."));
    }
}
