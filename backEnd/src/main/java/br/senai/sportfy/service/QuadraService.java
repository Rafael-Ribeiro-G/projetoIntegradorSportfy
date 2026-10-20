package br.senai.sportfy.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.senai.sportfy.domain.Esporte;
import br.senai.sportfy.domain.Local;
import br.senai.sportfy.domain.Quadra;
import br.senai.sportfy.dto.quadra.QuadraRequest;
import br.senai.sportfy.dto.quadra.QuadraResponse;
import br.senai.sportfy.exception.ConflitoException;
import br.senai.sportfy.exception.RecursoNaoEncontradoException;
import br.senai.sportfy.exception.RequisicaoInvalidaException;
import br.senai.sportfy.repository.EsporteRepository;
import br.senai.sportfy.repository.LocalRepository;
import br.senai.sportfy.repository.PartidaRepository;
import br.senai.sportfy.repository.QuadraRepository;

/** Quadras de um local. Só o dono do local cadastra, edita ou exclui. */
@Service
public class QuadraService {

    private final QuadraRepository quadraRepository;
    private final LocalRepository localRepository;
    private final EsporteRepository esporteRepository;
    private final PartidaRepository partidaRepository;

    public QuadraService(QuadraRepository quadraRepository, LocalRepository localRepository,
                         EsporteRepository esporteRepository, PartidaRepository partidaRepository) {
        this.quadraRepository = quadraRepository;
        this.localRepository = localRepository;
        this.esporteRepository = esporteRepository;
        this.partidaRepository = partidaRepository;
    }

    @Transactional(readOnly = true)
    public List<QuadraResponse> listar(UUID localId, Integer esporteId) {
        List<Quadra> quadras = localId != null ? quadraRepository.buscarDoLocal(localId) : quadraRepository.buscarTodas();
        return quadras.stream()
                .filter(q -> esporteId == null || q.getEsporte().getId().equals(esporteId))
                .map(QuadraResponse::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public QuadraResponse detalhe(UUID id) {
        return QuadraResponse.de(buscarEntidade(id));
    }

    @Transactional
    public QuadraResponse criar(QuadraRequest req, UUID usuarioId) {
        if (req.localId() == null) {
            throw new RequisicaoInvalidaException("Informe o localId da quadra.");
        }
        Local local = localRepository.findById(req.localId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Local não encontrado."));
        LocalService.verificarDono(local, usuarioId);

        Quadra quadra = new Quadra();
        quadra.setLocal(local);
        aplicar(quadra, req);
        return QuadraResponse.de(quadraRepository.save(quadra));
    }

    @Transactional
    public QuadraResponse atualizar(UUID id, QuadraRequest req, UUID usuarioId) {
        Quadra quadra = buscarEntidade(id);
        LocalService.verificarDono(quadra.getLocal(), usuarioId);
        aplicar(quadra, req);
        return QuadraResponse.de(quadra);
    }

    @Transactional
    public void excluir(UUID id, UUID usuarioId) {
        Quadra quadra = buscarEntidade(id);
        LocalService.verificarDono(quadra.getLocal(), usuarioId);
        if (partidaRepository.countByQuadraId(id) > 0) {
            throw new ConflitoException("Esta quadra tem partidas registradas e não pode ser excluída.");
        }
        quadraRepository.delete(quadra);
    }

    private Quadra buscarEntidade(UUID id) {
        return quadraRepository.buscarCompleta(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Quadra não encontrada."));
    }

    private void aplicar(Quadra quadra, QuadraRequest req) {
        Esporte esporte = esporteRepository.findById(req.esporteId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Esporte não encontrado."));
        quadra.setEsporte(esporte);
        quadra.setNome(req.nome().trim());
        quadra.setPrecoHora(req.precoHora());
    }
}
