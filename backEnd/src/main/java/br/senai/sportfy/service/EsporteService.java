package br.senai.sportfy.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.senai.sportfy.domain.Esporte;
import br.senai.sportfy.dto.esporte.EsporteRequest;
import br.senai.sportfy.dto.esporte.EsporteResponse;
import br.senai.sportfy.exception.ConflitoException;
import br.senai.sportfy.exception.RecursoNaoEncontradoException;
import br.senai.sportfy.repository.EsporteRepository;
import br.senai.sportfy.repository.EventoRepository;

@Service
public class EsporteService {

    private final EsporteRepository esporteRepository;
    private final EventoRepository eventoRepository;

    public EsporteService(EsporteRepository esporteRepository, EventoRepository eventoRepository) {
        this.esporteRepository = esporteRepository;
        this.eventoRepository = eventoRepository;
    }

    @Transactional(readOnly = true)
    public List<EsporteResponse> listar(boolean incluirInativos) {
        List<Esporte> esportes = incluirInativos
                ? esporteRepository.findAllByOrderByNomeAsc()
                : esporteRepository.findByAtivoTrueOrderByNomeAsc();
        return esportes.stream().map(EsporteResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public EsporteResponse buscar(Long id) {
        return EsporteResponse.de(buscarEntidade(id));
    }

    @Transactional
    public EsporteResponse criar(EsporteRequest req) {
        String nome = req.nome().trim();
        if (esporteRepository.existsByNomeIgnoreCase(nome)) {
            throw new ConflitoException("Já existe um esporte com este nome.");
        }
        Esporte esporte = new Esporte(nome, AuthService.textoOuNulo(req.descricao()), req.jogadoresSugeridos());
        esporte.setAtivo(req.ativo() == null || req.ativo());
        return EsporteResponse.de(esporteRepository.save(esporte));
    }

    @Transactional
    public EsporteResponse atualizar(Long id, EsporteRequest req) {
        Esporte esporte = buscarEntidade(id);
        String nome = req.nome().trim();
        if (esporteRepository.existsByNomeIgnoreCaseAndIdNot(nome, id)) {
            throw new ConflitoException("Já existe um esporte com este nome.");
        }
        esporte.setNome(nome);
        esporte.setDescricao(AuthService.textoOuNulo(req.descricao()));
        esporte.setJogadoresSugeridos(req.jogadoresSugeridos());
        if (req.ativo() != null) {
            esporte.setAtivo(req.ativo());
        }
        return EsporteResponse.de(esporte);
    }

    /**
     * Esportes já usados em eventos não são apagados (perderíamos o histórico):
     * eles são apenas desativados.
     */
    @Transactional
    public void excluir(Long id) {
        Esporte esporte = buscarEntidade(id);
        if (eventoRepository.countByEsporteId(id) > 0) {
            esporte.setAtivo(false);
        } else {
            esporteRepository.delete(esporte);
        }
    }

    Esporte buscarEntidade(Long id) {
        return esporteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Esporte não encontrado."));
    }
}
