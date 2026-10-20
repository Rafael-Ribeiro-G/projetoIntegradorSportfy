package br.senai.sportfy.service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.senai.sportfy.domain.Local;
import br.senai.sportfy.domain.Quadra;
import br.senai.sportfy.domain.Usuario;
import br.senai.sportfy.dto.local.LocalRequest;
import br.senai.sportfy.dto.local.LocalResponse;
import br.senai.sportfy.dto.quadra.QuadraResumo;
import br.senai.sportfy.exception.AcessoNegadoException;
import br.senai.sportfy.exception.ConflitoException;
import br.senai.sportfy.exception.RecursoNaoEncontradoException;
import br.senai.sportfy.exception.RequisicaoInvalidaException;
import br.senai.sportfy.repository.LocalRepository;
import br.senai.sportfy.repository.PartidaRepository;
import br.senai.sportfy.repository.QuadraRepository;
import br.senai.sportfy.repository.UsuarioRepository;
import br.senai.sportfy.util.GeoUtils;

/**
 * Locais (clubes, parques, escolas). Qualquer usuário logado pode cadastrar;
 * só quem cadastrou (o "dono") pode editar ou excluir.
 */
@Service
public class LocalService {

    private final LocalRepository localRepository;
    private final QuadraRepository quadraRepository;
    private final PartidaRepository partidaRepository;
    private final UsuarioRepository usuarioRepository;

    public LocalService(LocalRepository localRepository, QuadraRepository quadraRepository,
                        PartidaRepository partidaRepository, UsuarioRepository usuarioRepository) {
        this.localRepository = localRepository;
        this.quadraRepository = quadraRepository;
        this.partidaRepository = partidaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /** Lista locais; com latitude/longitude ordena pelos mais próximos (e filtra pelo raio, se houver). */
    @Transactional(readOnly = true)
    public List<LocalResponse> buscar(String texto, Double latitude, Double longitude, Double raioKm) {
        PartidaService.validarCoordenadas(latitude, longitude);
        if (raioKm != null && (latitude == null || raioKm <= 0 || raioKm > PartidaService.RAIO_MAXIMO_KM)) {
            throw new RequisicaoInvalidaException(
                    "Para filtrar por raio informe latitude e longitude, com raio entre 0 e "
                            + PartidaService.RAIO_MAXIMO_KM + " km.");
        }
        String termo = texto == null ? "" : texto.trim();
        List<Local> locais = localRepository.buscar(termo);

        if (latitude == null) {
            return locais.stream().map(l -> LocalResponse.de(l, null, null)).toList();
        }
        return locais.stream()
                .filter(l -> raioKm == null || (l.temPosicao() && distancia(l, latitude, longitude) <= raioKm))
                .sorted(Comparator.comparingDouble(l -> l.temPosicao() ? distancia(l, latitude, longitude)
                        : Double.MAX_VALUE))
                .map(l -> LocalResponse.de(l, l.temPosicao()
                        ? GeoUtils.arredondar(distancia(l, latitude, longitude)) : null, null))
                .toList();
    }

    /** Detalhe com as quadras do local. */
    @Transactional(readOnly = true)
    public LocalResponse detalhe(UUID id) {
        Local local = buscarEntidade(id);
        List<QuadraResumo> quadras = quadraRepository.buscarDoLocal(id).stream().map(QuadraResumo::de).toList();
        return LocalResponse.de(local, null, quadras);
    }

    @Transactional(readOnly = true)
    public List<LocalResponse> doUsuario(UUID usuarioId) {
        return localRepository.buscarDoDono(usuarioId).stream().map(l -> LocalResponse.de(l, null, null)).toList();
    }

    @Transactional
    public LocalResponse criar(LocalRequest req, UUID usuarioId) {
        PartidaService.validarCoordenadas(req.latitude(), req.longitude());
        Usuario dono = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));
        Local local = new Local();
        local.setDono(dono);
        aplicar(local, req);
        return LocalResponse.de(localRepository.save(local), null, List.of());
    }

    @Transactional
    public LocalResponse atualizar(UUID id, LocalRequest req, UUID usuarioId) {
        PartidaService.validarCoordenadas(req.latitude(), req.longitude());
        Local local = buscarEntidade(id);
        verificarDono(local, usuarioId);
        aplicar(local, req);
        List<QuadraResumo> quadras = quadraRepository.buscarDoLocal(id).stream().map(QuadraResumo::de).toList();
        return LocalResponse.de(local, null, quadras);
    }

    /** Exclui o local e as quadras dele, desde que nenhuma quadra tenha partidas. */
    @Transactional
    public void excluir(UUID id, UUID usuarioId) {
        Local local = buscarEntidade(id);
        verificarDono(local, usuarioId);
        List<Quadra> quadras = quadraRepository.buscarDoLocal(id);
        boolean temPartidas = quadras.stream().anyMatch(q -> partidaRepository.countByQuadraId(q.getId()) > 0);
        if (temPartidas) {
            throw new ConflitoException("Este local tem quadras com partidas registradas e não pode ser excluído.");
        }
        quadraRepository.deleteAll(quadras);
        localRepository.delete(local);
    }

    Local buscarEntidade(UUID id) {
        return localRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Local não encontrado."));
    }

    /** Local sem dono (cadastrado direto no banco) só pode ser alterado no banco. */
    static void verificarDono(Local local, UUID usuarioId) {
        if (!local.pertenceA(usuarioId)) {
            throw new AcessoNegadoException("Apenas quem cadastrou o local pode alterá-lo.");
        }
    }

    private static void aplicar(Local local, LocalRequest req) {
        local.setNome(req.nome().trim());
        local.setEndereco(req.endereco().trim());
        local.definirPosicao(req.latitude(), req.longitude());
    }

    private static double distancia(Local l, double lat, double lon) {
        return GeoUtils.distanciaKm(lat, lon, l.getLatitude(), l.getLongitude());
    }
}
