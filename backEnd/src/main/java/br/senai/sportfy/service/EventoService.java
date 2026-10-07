package br.senai.sportfy.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.senai.sportfy.domain.Esporte;
import br.senai.sportfy.domain.Evento;
import br.senai.sportfy.domain.Participacao;
import br.senai.sportfy.domain.Usuario;
import br.senai.sportfy.domain.enums.NivelHabilidade;
import br.senai.sportfy.domain.enums.StatusEvento;
import br.senai.sportfy.domain.enums.StatusParticipacao;
import br.senai.sportfy.dto.PaginaResponse;
import br.senai.sportfy.dto.esporte.EsporteResumo;
import br.senai.sportfy.dto.evento.EventoFiltro;
import br.senai.sportfy.dto.evento.EventoRequest;
import br.senai.sportfy.dto.evento.EventoResponse;
import br.senai.sportfy.dto.evento.MeusEventosResponse;
import br.senai.sportfy.dto.evento.ParticipanteResponse;
import br.senai.sportfy.exception.AcessoNegadoException;
import br.senai.sportfy.exception.ConflitoException;
import br.senai.sportfy.exception.RecursoNaoEncontradoException;
import br.senai.sportfy.exception.RegraNegocioException;
import br.senai.sportfy.exception.RequisicaoInvalidaException;
import br.senai.sportfy.repository.EsporteRepository;
import br.senai.sportfy.repository.EventoRepository;
import br.senai.sportfy.repository.EventoSpecifications;
import br.senai.sportfy.repository.ParticipacaoRepository;
import br.senai.sportfy.repository.UsuarioRepository;
import br.senai.sportfy.security.UsuarioAutenticado;
import br.senai.sportfy.util.GeoUtils;

/**
 * Regras de negócio dos eventos/partidas.
 *
 * Principais regras:
 * - quem cria o evento é o organizador e já entra como participante confirmado;
 * - só organizador ou ADMIN editam/cancelam;
 * - não é possível entrar em evento cancelado, já iniciado ou lotado;
 * - o organizador não pode "sair" do próprio evento (deve cancelar);
 * - o limite de vagas não pode ficar abaixo do número de confirmados.
 */
@Service
public class EventoService {

    static final double RAIO_MAXIMO_KM = 500.0;
    static final int MAX_SUGESTOES = 20;
    private static final int CANDIDATOS_SUGESTAO = 300;
    private static final int ANTECEDENCIA_MAXIMA_DIAS = 365;

    private final EventoRepository eventoRepository;
    private final ParticipacaoRepository participacaoRepository;
    private final EsporteRepository esporteRepository;
    private final UsuarioRepository usuarioRepository;
    private final Clock clock;

    public EventoService(EventoRepository eventoRepository, ParticipacaoRepository participacaoRepository,
                         EsporteRepository esporteRepository, UsuarioRepository usuarioRepository, Clock clock) {
        this.eventoRepository = eventoRepository;
        this.participacaoRepository = participacaoRepository;
        this.esporteRepository = esporteRepository;
        this.usuarioRepository = usuarioRepository;
        this.clock = clock;
    }

    // ================================================================ consultas

    @Transactional(readOnly = true)
    public PaginaResponse<EventoResponse> buscar(EventoFiltro filtro, Pageable pageable, Long usuarioLogadoId) {
        validarFiltro(filtro);
        Specification<Evento> spec = EventoSpecifications.comFiltro(filtro, agora());
        Page<Evento> pagina = (filtro.temLocalizacao() && filtro.raioKm() != null)
                ? buscarDentroDoRaio(spec, filtro, pageable)
                : eventoRepository.findAll(spec, pageable);
        Set<Long> participa = idsEventosDoUsuario(usuarioLogadoId);
        List<EventoResponse> conteudo = paraResponses(pagina.getContent(), filtro.latitude(), filtro.longitude(),
                usuarioLogadoId == null ? null : participa);
        return PaginaResponse.de(pagina, conteudo);
    }

    @Transactional(readOnly = true)
    public EventoResponse buscarPorId(Long id, Long usuarioLogadoId) {
        Evento evento = eventoRepository.buscarCompleto(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Evento não encontrado."));
        Boolean participa = usuarioLogadoId == null ? null
                : participacaoRepository.existsByEventoIdAndUsuarioIdAndStatus(id, usuarioLogadoId,
                        StatusParticipacao.CONFIRMADO);
        return paraResponse(evento, contarConfirmados(id), null, null, participa);
    }

    @Transactional(readOnly = true)
    public List<ParticipanteResponse> listarParticipantes(Long eventoId) {
        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Evento não encontrado."));
        Long organizadorId = evento.getOrganizador().getId();
        return participacaoRepository.buscarPorEvento(eventoId, StatusParticipacao.CONFIRMADO).stream()
                .map(p -> new ParticipanteResponse(
                        p.getUsuario().getId(),
                        p.getUsuario().getNome(),
                        p.getUsuario().getNivel(),
                        p.getUsuario().getId().equals(organizadorId),
                        p.getAtualizadoEm()))
                .toList();
    }

    @Transactional(readOnly = true)
    public MeusEventosResponse meusEventos(Long usuarioId) {
        List<Evento> organizados = eventoRepository.buscarOrganizadosPor(usuarioId);
        Set<Long> idsOrganizados = organizados.stream().map(Evento::getId).collect(Collectors.toSet());

        List<Evento> participando = participacaoRepository
                .buscarPorUsuario(usuarioId, StatusParticipacao.CONFIRMADO).stream()
                .map(Participacao::getEvento)
                .filter(e -> !idsOrganizados.contains(e.getId()))
                .toList();

        Set<Long> participa = idsEventosDoUsuario(usuarioId);
        return new MeusEventosResponse(
                paraResponses(organizados, null, null, participa),
                paraResponses(participando, null, null, participa));
    }

    /**
     * Sugestões personalizadas (Home e chatbot): eventos futuros com vaga,
     * dos esportes favoritos do usuário, ordenados pela distância até ele.
     */
    @Transactional(readOnly = true)
    public List<EventoResponse> sugestoes(Long usuarioId, int limite) {
        int quantidade = Math.max(1, Math.min(limite, MAX_SUGESTOES));
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));

        Set<Long> favoritos = usuario.getEsportesFavoritos().stream().map(Esporte::getId).collect(Collectors.toSet());
        Set<Long> participa = idsEventosDoUsuario(usuarioId);

        List<Evento> candidatos = eventoRepository.buscarFuturos(StatusEvento.ATIVO, agora(),
                        PageRequest.of(0, CANDIDATOS_SUGESTAO)).stream()
                .filter(e -> !e.isOrganizadoPor(usuarioId))
                .filter(e -> !participa.contains(e.getId()))
                .toList();

        Map<Long, Long> confirmados = contarConfirmados(candidatos.stream().map(Evento::getId).toList());
        List<Evento> comVaga = candidatos.stream()
                .filter(e -> confirmados.getOrDefault(e.getId(), 0L) < e.getMaxParticipantes())
                .toList();

        List<Evento> preferidos = comVaga.stream()
                .filter(e -> favoritos.contains(e.getEsporte().getId()))
                .toList();
        // se não houver nada dos esportes favoritos, sugere qualquer esporte
        List<Evento> escolhidos = preferidos.isEmpty() ? comVaga : preferidos;

        Double lat = usuario.getLatitude();
        Double lon = usuario.getLongitude();
        if (lat != null && lon != null) {
            escolhidos = escolhidos.stream()
                    .sorted(Comparator.comparingDouble(e -> distanciaOuInfinito(e, lat, lon)))
                    .toList();
        }

        return escolhidos.stream()
                .limit(quantidade)
                .map(e -> paraResponse(e, confirmados.getOrDefault(e.getId(), 0L), lat, lon, false))
                .toList();
    }

    /**
     * Busca por raio: o banco devolve só o "quadrado" em volta do ponto (rápido) e aqui
     * removemos os cantos que ficam fora do círculo (distância exata). A paginação é feita em memória,
     * o que é tranquilo porque o filtro de região já reduz bastante o volume.
     */
    private Page<Evento> buscarDentroDoRaio(Specification<Evento> spec, EventoFiltro f, Pageable pageable) {
        List<Evento> dentro = eventoRepository.findAll(spec, pageable.getSort()).stream()
                .filter(e -> e.getLatitude() != null && e.getLongitude() != null)
                .filter(e -> GeoUtils.distanciaKm(f.latitude(), f.longitude(), e.getLatitude(), e.getLongitude())
                        <= f.raioKm())
                .toList();
        int inicio = (int) Math.min(pageable.getOffset(), dentro.size());
        int fim = Math.min(inicio + pageable.getPageSize(), dentro.size());
        return new PageImpl<>(dentro.subList(inicio, fim), pageable, dentro.size());
    }

    // ================================================================ comandos

    @Transactional
    public EventoResponse criar(EventoRequest req, Long organizadorId) {
        validarCoordenadas(req.latitude(), req.longitude());
        validarData(req.dataHoraInicio());

        Esporte esporte = buscarEsporteAtivo(req.esporteId());
        Usuario organizador = usuarioRepository.findById(organizadorId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));

        Evento evento = new Evento();
        evento.setOrganizador(organizador);
        aplicarDados(evento, req, esporte);
        evento.setStatus(StatusEvento.ATIVO);
        evento = eventoRepository.save(evento);

        // o organizador já ocupa uma vaga
        participacaoRepository.save(new Participacao(evento, organizador));

        return paraResponse(evento, 1, null, null, true);
    }

    @Transactional
    public EventoResponse atualizar(Long eventoId, EventoRequest req, UsuarioAutenticado usuario) {
        Evento evento = buscarParaAtualizacao(eventoId);
        verificarPermissao(evento, usuario);

        if (evento.isCancelado()) {
            throw new RegraNegocioException("Não é possível editar um evento cancelado.");
        }
        if (evento.jaComecou(agora())) {
            throw new RegraNegocioException("Não é possível editar um evento que já começou.");
        }
        validarCoordenadas(req.latitude(), req.longitude());
        validarData(req.dataHoraInicio());

        long confirmados = contarConfirmados(eventoId);
        if (req.maxParticipantes() < confirmados) {
            throw new RegraNegocioException("O evento já tem " + confirmados
                    + " participantes confirmados; o limite não pode ser menor que isso.");
        }

        Esporte esporte = evento.getEsporte().getId().equals(req.esporteId())
                ? evento.getEsporte()
                : buscarEsporteAtivo(req.esporteId());
        aplicarDados(evento, req, esporte);

        boolean participa = participacaoRepository.existsByEventoIdAndUsuarioIdAndStatus(eventoId, usuario.id(),
                StatusParticipacao.CONFIRMADO);
        return paraResponse(evento, confirmados, null, null, participa);
    }

    @Transactional
    public void cancelar(Long eventoId, UsuarioAutenticado usuario) {
        Evento evento = buscarParaAtualizacao(eventoId);
        verificarPermissao(evento, usuario);

        if (evento.isCancelado()) {
            throw new RegraNegocioException("O evento já está cancelado.");
        }
        if (!agora().isBefore(evento.getDataHoraFim())) {
            throw new RegraNegocioException("Não é possível cancelar um evento já encerrado.");
        }
        evento.setStatus(StatusEvento.CANCELADO);
    }

    @Transactional
    public EventoResponse participar(Long eventoId, Long usuarioId) {
        // trava o evento: duas pessoas não conseguem pegar a última vaga ao mesmo tempo
        Evento evento = buscarParaAtualizacao(eventoId);

        if (evento.isCancelado()) {
            throw new RegraNegocioException("Este evento foi cancelado.");
        }
        if (evento.jaComecou(agora())) {
            throw new RegraNegocioException("Este evento já começou; não é mais possível entrar.");
        }

        Participacao existente = participacaoRepository.findByEventoIdAndUsuarioId(eventoId, usuarioId).orElse(null);
        if (existente != null && existente.isConfirmada()) {
            throw new ConflitoException("Você já está confirmado neste evento.");
        }

        long confirmados = contarConfirmados(eventoId);
        if (confirmados >= evento.getMaxParticipantes()) {
            throw new RegraNegocioException("Evento lotado: não há mais vagas.");
        }

        if (existente != null) {
            existente.setStatus(StatusParticipacao.CONFIRMADO);
        } else {
            Usuario usuario = usuarioRepository.findById(usuarioId)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));
            participacaoRepository.save(new Participacao(evento, usuario));
        }

        return paraResponse(evento, confirmados + 1, null, null, true);
    }

    @Transactional
    public EventoResponse sair(Long eventoId, Long usuarioId) {
        Evento evento = buscarParaAtualizacao(eventoId);

        if (evento.isOrganizadoPor(usuarioId)) {
            throw new RegraNegocioException("O organizador não pode sair do próprio evento. Cancele o evento.");
        }
        Participacao participacao = participacaoRepository.findByEventoIdAndUsuarioId(eventoId, usuarioId)
                .filter(Participacao::isConfirmada)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Você não está participando deste evento."));
        if (evento.jaComecou(agora())) {
            throw new RegraNegocioException("O evento já começou; não é possível sair.");
        }

        // conta ANTES de mudar o status (o Hibernate grava a mudança antes de consultas na mesma transação)
        long confirmados = contarConfirmados(eventoId);
        participacao.setStatus(StatusParticipacao.CANCELADO);
        return paraResponse(evento, Math.max(confirmados - 1, 0), null, null, false);
    }

    /**
     * Usado na exclusão de conta (LGPD): cancela os eventos futuros que a pessoa organiza
     * e as participações dela em eventos futuros.
     */
    @Transactional
    public void encerrarVinculosFuturos(Long usuarioId) {
        LocalDateTime agora = agora();
        eventoRepository.buscarFuturosAtivosDoOrganizador(usuarioId, agora)
                .forEach(e -> e.setStatus(StatusEvento.CANCELADO));
        participacaoRepository.findByUsuarioIdAndStatus(usuarioId, StatusParticipacao.CONFIRMADO).stream()
                .filter(p -> p.getEvento().getDataHoraInicio().isAfter(agora))
                .forEach(p -> p.setStatus(StatusParticipacao.CANCELADO));
    }

    // ================================================================ auxiliares

    private LocalDateTime agora() {
        return LocalDateTime.now(clock);
    }

    private Evento buscarParaAtualizacao(Long eventoId) {
        return eventoRepository.buscarParaAtualizacao(eventoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Evento não encontrado."));
    }

    private Esporte buscarEsporteAtivo(Long esporteId) {
        Esporte esporte = esporteRepository.findById(esporteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Esporte não encontrado."));
        if (!esporte.isAtivo()) {
            throw new RegraNegocioException("Este esporte não está disponível para novos eventos.");
        }
        return esporte;
    }

    private void verificarPermissao(Evento evento, UsuarioAutenticado usuario) {
        if (!evento.isOrganizadoPor(usuario.id()) && !usuario.isAdmin()) {
            throw new AcessoNegadoException("Apenas o organizador do evento pode fazer esta alteração.");
        }
    }

    private void aplicarDados(Evento evento, EventoRequest req, Esporte esporte) {
        evento.setTitulo(req.titulo().trim());
        evento.setDescricao(AuthService.textoOuNulo(req.descricao()));
        evento.setEsporte(esporte);
        evento.setDataHoraInicio(req.dataHoraInicio());
        evento.setDuracaoMinutos(req.duracaoMinutos());
        evento.setLocalNome(req.localNome().trim());
        evento.setEndereco(AuthService.textoOuNulo(req.endereco()));
        evento.setCidade(req.cidade().trim());
        evento.setLatitude(req.latitude());
        evento.setLongitude(req.longitude());
        evento.setMaxParticipantes(req.maxParticipantes());
        evento.setNivel(req.nivel() == null ? NivelHabilidade.TODOS : req.nivel());
    }

    private void validarData(LocalDateTime dataHoraInicio) {
        LocalDateTime agora = agora();
        if (!dataHoraInicio.isAfter(agora)) {
            throw new RegraNegocioException("A data do evento deve ser no futuro.");
        }
        if (dataHoraInicio.isAfter(agora.plusDays(ANTECEDENCIA_MAXIMA_DIAS))) {
            throw new RegraNegocioException("O evento pode ser marcado com no máximo 1 ano de antecedência.");
        }
    }

    static void validarCoordenadas(Double latitude, Double longitude) {
        if ((latitude == null) != (longitude == null)) {
            throw new RequisicaoInvalidaException("Informe latitude e longitude juntas (ou nenhuma das duas).");
        }
    }

    private void validarFiltro(EventoFiltro f) {
        validarCoordenadas(f.latitude(), f.longitude());
        if (f.raioKm() != null) {
            if (!f.temLocalizacao()) {
                throw new RequisicaoInvalidaException("Para filtrar por raio, informe latitude e longitude.");
            }
            if (f.raioKm() <= 0 || f.raioKm() > RAIO_MAXIMO_KM) {
                throw new RequisicaoInvalidaException("O raio deve ser maior que 0 e no máximo " + RAIO_MAXIMO_KM + " km.");
            }
        }
        if (f.dataInicio() != null && f.dataFim() != null && f.dataFim().isBefore(f.dataInicio())) {
            throw new RequisicaoInvalidaException("dataFim não pode ser anterior a dataInicio.");
        }
    }

    private long contarConfirmados(Long eventoId) {
        return participacaoRepository.countByEventoIdAndStatus(eventoId, StatusParticipacao.CONFIRMADO);
    }

    private Map<Long, Long> contarConfirmados(Collection<Long> eventoIds) {
        Map<Long, Long> mapa = new HashMap<>();
        if (eventoIds.isEmpty()) {
            return mapa;
        }
        for (Object[] linha : participacaoRepository.contarPorEventos(eventoIds, StatusParticipacao.CONFIRMADO)) {
            mapa.put((Long) linha[0], ((Number) linha[1]).longValue());
        }
        return mapa;
    }

    private Set<Long> idsEventosDoUsuario(Long usuarioId) {
        if (usuarioId == null) {
            return Set.of();
        }
        return new HashSet<>(participacaoRepository.buscarIdsEventosDoUsuario(usuarioId,
                StatusParticipacao.CONFIRMADO));
    }

    /** participa == null significa "visitante não logado" (campo omitido no JSON). */
    private List<EventoResponse> paraResponses(List<Evento> eventos, Double lat, Double lon, Set<Long> participa) {
        Map<Long, Long> confirmados = contarConfirmados(eventos.stream().map(Evento::getId).toList());
        return eventos.stream()
                .map(e -> paraResponse(e, confirmados.getOrDefault(e.getId(), 0L), lat, lon,
                        participa == null ? null : participa.contains(e.getId())))
                .toList();
    }

    private EventoResponse paraResponse(Evento e, long confirmados, Double lat, Double lon, Boolean participa) {
        Double distancia = null;
        if (lat != null && lon != null && e.getLatitude() != null && e.getLongitude() != null) {
            distancia = GeoUtils.arredondar(GeoUtils.distanciaKm(lat, lon, e.getLatitude(), e.getLongitude()));
        }
        long vagas = Math.max(0, e.getMaxParticipantes() - confirmados);
        return new EventoResponse(
                e.getId(),
                e.getTitulo(),
                e.getDescricao(),
                EsporteResumo.de(e.getEsporte()),
                new EventoResponse.OrganizadorResumo(e.getOrganizador().getId(), e.getOrganizador().getNome()),
                e.getDataHoraInicio(),
                e.getDataHoraFim(),
                e.getDuracaoMinutos(),
                e.getLocalNome(),
                e.getEndereco(),
                e.getCidade(),
                e.getLatitude(),
                e.getLongitude(),
                e.getMaxParticipantes(),
                confirmados,
                vagas,
                e.getNivel(),
                e.calcularSituacao(agora(), confirmados),
                distancia,
                participa,
                e.getCriadoEm());
    }

    private static double distanciaOuInfinito(Evento e, double lat, double lon) {
        if (e.getLatitude() == null || e.getLongitude() == null) {
            return Double.MAX_VALUE;
        }
        return GeoUtils.distanciaKm(lat, lon, e.getLatitude(), e.getLongitude());
    }
}
