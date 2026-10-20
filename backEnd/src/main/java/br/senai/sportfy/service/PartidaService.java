package br.senai.sportfy.service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.senai.sportfy.domain.Esporte;
import br.senai.sportfy.domain.Local;
import br.senai.sportfy.domain.ParticipanteId;
import br.senai.sportfy.domain.Partida;
import br.senai.sportfy.domain.PartidaParticipante;
import br.senai.sportfy.domain.Quadra;
import br.senai.sportfy.domain.Usuario;
import br.senai.sportfy.domain.enums.StatusConfirmacao;
import br.senai.sportfy.domain.enums.StatusPartida;
import br.senai.sportfy.dto.PaginaResponse;
import br.senai.sportfy.dto.esporte.EsporteResponse;
import br.senai.sportfy.dto.local.LocalResumo;
import br.senai.sportfy.dto.partida.MinhasPartidasResponse;
import br.senai.sportfy.dto.partida.ParticipanteResponse;
import br.senai.sportfy.dto.partida.PartidaFiltro;
import br.senai.sportfy.dto.partida.PartidaRequest;
import br.senai.sportfy.dto.partida.PartidaResponse;
import br.senai.sportfy.dto.usuario.UsuarioResumo;
import br.senai.sportfy.exception.AcessoNegadoException;
import br.senai.sportfy.exception.ConflitoException;
import br.senai.sportfy.exception.RecursoNaoEncontradoException;
import br.senai.sportfy.exception.RegraNegocioException;
import br.senai.sportfy.exception.RequisicaoInvalidaException;
import br.senai.sportfy.repository.EsporteRepository;
import br.senai.sportfy.repository.LocalRepository;
import br.senai.sportfy.repository.PartidaParticipanteRepository;
import br.senai.sportfy.repository.PartidaRepository;
import br.senai.sportfy.repository.PartidaSpecifications;
import br.senai.sportfy.repository.QuadraRepository;
import br.senai.sportfy.repository.UsuarioRepository;
import br.senai.sportfy.util.GeoUtils;

/**
 * Regras de negócio das partidas (qualquer atividade: pelada, corrida, treino, caminhada...).
 *
 * - a partida acontece num local, de um esporte; a quadra é opcional;
 * - quem cria a partida já entra como participante confirmado;
 * - só quem criou edita ou cancela;
 * - a mesma quadra não pode ter duas partidas no mesmo horário;
 * - partidas em locais privados não aparecem nas buscas de outras pessoas (quem tem o id vê);
 * - não dá para entrar em partida cancelada, já iniciada ou lotada;
 * - quem criou não pode "sair" da própria partida (deve cancelar);
 * - o limite de jogadores não pode ficar abaixo do número de confirmados.
 */
@Service
public class PartidaService {

    static final double RAIO_MAXIMO_KM = 500.0;
    static final int MAX_SUGESTOES = 20;
    static final int DURACAO_MINIMA_MINUTOS = 15;
    static final int DURACAO_MAXIMA_HORAS = 12;
    private static final int ANTECEDENCIA_MAXIMA_DIAS = 365;
    private static final Sort ORDEM_PADRAO = Sort.by(Sort.Order.asc("dataHoraInicio"), Sort.Order.asc("id"));

    private final PartidaRepository partidaRepository;
    private final PartidaParticipanteRepository participanteRepository;
    private final QuadraRepository quadraRepository;
    private final LocalRepository localRepository;
    private final EsporteRepository esporteRepository;
    private final UsuarioRepository usuarioRepository;
    private final Clock clock;

    public PartidaService(PartidaRepository partidaRepository, PartidaParticipanteRepository participanteRepository,
                          QuadraRepository quadraRepository, LocalRepository localRepository,
                          EsporteRepository esporteRepository, UsuarioRepository usuarioRepository, Clock clock) {
        this.partidaRepository = partidaRepository;
        this.participanteRepository = participanteRepository;
        this.quadraRepository = quadraRepository;
        this.localRepository = localRepository;
        this.esporteRepository = esporteRepository;
        this.usuarioRepository = usuarioRepository;
        this.clock = clock;
    }

    // ================================================================ consultas

    /**
     * Busca com filtros. Com latitude/longitude, calcula a distância de cada partida;
     * com raioKm, devolve só as que estão dentro do raio. Paginação feita aqui
     * (o volume de partidas futuras de um projeto deste tamanho é pequeno).
     */
    @Transactional(readOnly = true)
    public PaginaResponse<PartidaResponse> buscar(PartidaFiltro filtro, Pageable pageable, UUID usuarioLogadoId) {
        validarFiltro(filtro);
        List<Partida> encontradas = partidaRepository.findAll(
                PartidaSpecifications.comFiltro(filtro, agora(), usuarioLogadoId), ORDEM_PADRAO);

        if (filtro.temLocalizacao() && filtro.raioKm() != null) {
            encontradas = encontradas.stream()
                    .filter(p -> p.getLocal().temPosicao())
                    .filter(p -> distancia(p, filtro.latitude(), filtro.longitude()) <= filtro.raioKm())
                    .toList();
        }

        int inicio = (int) Math.min(pageable.getOffset(), encontradas.size());
        int fim = Math.min(inicio + pageable.getPageSize(), encontradas.size());
        List<Partida> pagina = encontradas.subList(inicio, fim);

        Set<UUID> participa = idsPartidasDoUsuario(usuarioLogadoId);
        List<PartidaResponse> conteudo = paraResponses(pagina, filtro.latitude(), filtro.longitude(),
                usuarioLogadoId == null ? null : participa);
        return PaginaResponse.de(new PageImpl<>(pagina, pageable, encontradas.size()), conteudo);
    }

    @Transactional(readOnly = true)
    public PartidaResponse buscarPorId(UUID id, UUID usuarioLogadoId) {
        Partida partida = partidaRepository.buscarCompleta(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Partida não encontrada."));
        Boolean participa = usuarioLogadoId == null ? null : estaConfirmado(id, usuarioLogadoId);
        return paraResponse(partida, contarConfirmados(id), null, null, participa);
    }

    @Transactional(readOnly = true)
    public List<ParticipanteResponse> listarParticipantes(UUID partidaId) {
        Partida partida = partidaRepository.findById(partidaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Partida não encontrada."));
        UUID criadorId = partida.getCriador().getId();
        return participanteRepository.buscarPorPartida(partidaId, StatusConfirmacao.CONFIRMADO).stream()
                .map(pp -> new ParticipanteResponse(
                        pp.getUsuario().getId(),
                        pp.getUsuario().getNome(),
                        pp.getUsuario().getFotoUrl(),
                        pp.getUsuario().getNivelHabilidade(),
                        pp.getUsuario().getId().equals(criadorId)))
                // quem criou aparece primeiro
                .sorted(Comparator.comparing((ParticipanteResponse p) -> !p.criador()))
                .toList();
    }

    @Transactional(readOnly = true)
    public MinhasPartidasResponse minhasPartidas(UUID usuarioId) {
        List<Partida> criadas = partidaRepository.buscarCriadasPor(usuarioId);
        Set<UUID> idsCriadas = criadas.stream().map(Partida::getId).collect(Collectors.toSet());

        Set<UUID> participa = idsPartidasDoUsuario(usuarioId);
        Set<UUID> idsParticipando = participa.stream()
                .filter(id -> !idsCriadas.contains(id))
                .collect(Collectors.toSet());
        List<Partida> participando = idsParticipando.isEmpty() ? List.of()
                : partidaRepository.findAll((root, q, cb) -> root.get("id").in(idsParticipando),
                        Sort.by(Sort.Order.desc("dataHoraInicio")));

        return new MinhasPartidasResponse(
                paraResponses(criadas, null, null, participa),
                paraResponses(participando, null, null, participa));
    }

    /**
     * Sugestões (Home e chatbot): próximas partidas com vaga em que o usuário ainda não está.
     * Sem esporteId, as dos esportes favoritos do usuário vêm primeiro (se houver alguma).
     * Com latitude/longitude, ordena pela distância.
     */
    @Transactional(readOnly = true)
    public List<PartidaResponse> sugestoes(UUID usuarioId, Integer esporteId, Double latitude, Double longitude,
                                           int limite) {
        validarCoordenadas(latitude, longitude);
        int quantidade = Math.max(1, Math.min(limite, MAX_SUGESTOES));
        Set<UUID> participa = idsPartidasDoUsuario(usuarioId);

        PartidaFiltro filtro = new PartidaFiltro(esporteId, null, null, null, null, agora(), null, null, null, null,
                false, false);
        List<Partida> candidatas = partidaRepository.findAll(
                        PartidaSpecifications.comFiltro(filtro, agora(), usuarioId), ORDEM_PADRAO).stream()
                .filter(p -> !p.isCriadaPor(usuarioId))
                .filter(p -> !participa.contains(p.getId()))
                .toList();

        Map<UUID, Long> confirmados = contarConfirmados(candidatas.stream().map(Partida::getId).toList());
        List<Partida> comVaga = candidatas.stream()
                .filter(p -> !p.estaLotada(confirmados.getOrDefault(p.getId(), 0L)))
                .toList();

        if (esporteId == null) {
            Set<Integer> favoritos = usuarioRepository.findById(usuarioId)
                    .map(u -> u.getEsportesFavoritos().stream().map(Esporte::getId).collect(Collectors.toSet()))
                    .orElse(Set.of());
            List<Partida> preferidas = comVaga.stream()
                    .filter(p -> favoritos.contains(p.getEsporte().getId()))
                    .toList();
            // se não houver nada dos favoritos, sugere qualquer esporte
            if (!preferidas.isEmpty()) {
                comVaga = preferidas;
            }
        }

        if (latitude != null && longitude != null) {
            comVaga = comVaga.stream()
                    .sorted(Comparator.comparingDouble(p -> distanciaOuInfinito(p, latitude, longitude)))
                    .toList();
        }
        return comVaga.stream()
                .limit(quantidade)
                .map(p -> paraResponse(p, confirmados.getOrDefault(p.getId(), 0L), latitude, longitude, false))
                .toList();
    }

    // ================================================================ comandos

    @Transactional
    public PartidaResponse criar(PartidaRequest req, UUID criadorId) {
        validarHorario(req.dataHoraInicio(), req.dataHoraFim());
        Onde onde = resolverOnde(req, null);
        if (onde.quadra() != null) {
            verificarQuadraLivre(onde.quadra().getId(), req.dataHoraInicio(), req.dataHoraFim(), null);
        }

        Usuario criador = usuarioRepository.findById(criadorId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));

        Partida partida = new Partida();
        partida.setCriador(criador);
        aplicarOnde(partida, onde);
        aplicarTextos(partida, req);
        partida.setDataHoraInicio(req.dataHoraInicio());
        partida.setDataHoraFim(req.dataHoraFim());
        partida.setMaxJogadores(req.maxJogadores() != null ? req.maxJogadores()
                : onde.esporte().getQuantidadeJogadoresPadrao());
        partida.setStatus(StatusPartida.ABERTA);
        partida = partidaRepository.save(partida);

        // quem cria já ocupa uma vaga
        participanteRepository.save(new PartidaParticipante(partida, criador));

        return paraResponse(partida, 1, null, null, true);
    }

    @Transactional
    public PartidaResponse atualizar(UUID partidaId, PartidaRequest req, UUID usuarioId) {
        Partida partida = buscarParaAtualizacao(partidaId);
        verificarCriador(partida, usuarioId);

        if (partida.isCancelada()) {
            throw new RegraNegocioException("Não é possível editar uma partida cancelada.");
        }
        if (partida.jaComecou(agora())) {
            throw new RegraNegocioException("Não é possível editar uma partida que já começou.");
        }
        validarHorario(req.dataHoraInicio(), req.dataHoraFim());

        Onde onde = resolverOnde(req, partida.getQuadra());
        if (onde.quadra() != null) {
            verificarQuadraLivre(onde.quadra().getId(), req.dataHoraInicio(), req.dataHoraFim(), partidaId);
        }

        long confirmados = contarConfirmados(partidaId);
        Integer max = req.maxJogadores() != null ? req.maxJogadores() : partida.getMaxJogadores();
        if (max != null && max < confirmados) {
            throw new RegraNegocioException("A partida já tem " + confirmados
                    + " jogadores confirmados; o limite não pode ser menor que isso.");
        }

        aplicarOnde(partida, onde);
        aplicarTextos(partida, req);
        partida.setDataHoraInicio(req.dataHoraInicio());
        partida.setDataHoraFim(req.dataHoraFim());
        partida.setMaxJogadores(max);

        return paraResponse(partida, confirmados, null, null, estaConfirmado(partidaId, usuarioId));
    }

    /** Cancela (status "cancelada"); o registro fica no banco para histórico. */
    @Transactional
    public void cancelar(UUID partidaId, UUID usuarioId) {
        Partida partida = buscarParaAtualizacao(partidaId);
        verificarCriador(partida, usuarioId);

        if (partida.isCancelada()) {
            throw new RegraNegocioException("A partida já está cancelada.");
        }
        if (partida.jaTerminou(agora())) {
            throw new RegraNegocioException("Não é possível cancelar uma partida já encerrada.");
        }
        partida.setStatus(StatusPartida.CANCELADA);
    }

    @Transactional
    public PartidaResponse participar(UUID partidaId, UUID usuarioId) {
        // trava a partida: duas pessoas não conseguem pegar a última vaga ao mesmo tempo
        Partida partida = buscarParaAtualizacao(partidaId);

        if (partida.isCancelada()) {
            throw new RegraNegocioException("Esta partida foi cancelada.");
        }
        if (partida.jaComecou(agora())) {
            throw new RegraNegocioException("Esta partida já começou; não é mais possível entrar.");
        }

        PartidaParticipante existente = participanteRepository
                .findById(new ParticipanteId(partidaId, usuarioId)).orElse(null);
        if (existente != null && existente.isConfirmado()) {
            throw new ConflitoException("Você já está confirmado nesta partida.");
        }

        long confirmados = contarConfirmados(partidaId);
        if (partida.estaLotada(confirmados)) {
            throw new RegraNegocioException("Partida lotada: não há mais vagas.");
        }

        if (existente != null) {
            // havia um registro "pendente" (ex.: criado direto no banco): confirma
            existente.setStatusConfirmacao(StatusConfirmacao.CONFIRMADO);
        } else {
            Usuario usuario = usuarioRepository.findById(usuarioId)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));
            participanteRepository.save(new PartidaParticipante(partida, usuario));
        }
        return paraResponse(partida, confirmados + 1, null, null, true);
    }

    @Transactional
    public PartidaResponse sair(UUID partidaId, UUID usuarioId) {
        Partida partida = buscarParaAtualizacao(partidaId);

        if (partida.isCriadaPor(usuarioId)) {
            throw new RegraNegocioException("Quem criou a partida não pode sair dela. Cancele a partida.");
        }
        PartidaParticipante participacao = participanteRepository
                .findById(new ParticipanteId(partidaId, usuarioId))
                .filter(PartidaParticipante::isConfirmado)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Você não está participando desta partida."));
        if (partida.jaComecou(agora())) {
            throw new RegraNegocioException("A partida já começou; não é possível sair.");
        }

        // conta ANTES de apagar (o Hibernate grava a mudança antes de consultas na mesma transação)
        long confirmados = contarConfirmados(partidaId);
        participanteRepository.delete(participacao);
        return paraResponse(partida, Math.max(confirmados - 1, 0), null, null, false);
    }

    // ================================================================ auxiliares

    private LocalDateTime agora() {
        return LocalDateTime.now(clock);
    }

    private Partida buscarParaAtualizacao(UUID partidaId) {
        return partidaRepository.buscarParaAtualizacao(partidaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Partida não encontrada."));
    }

    /** Onde a partida acontece: local e esporte sempre; quadra só quando informada. */
    record Onde(Local local, Esporte esporte, Quadra quadra) {
    }

    /**
     * Com quadraId: local e esporte vêm da quadra (se localId/esporteId vierem, precisam bater).
     * Sem quadraId: localId e esporteId são obrigatórios.
     * quadraAtual: na edição, a quadra que a partida já tem (evita buscá-la de novo).
     */
    private Onde resolverOnde(PartidaRequest req, Quadra quadraAtual) {
        if (req.quadraId() != null) {
            Quadra quadra = quadraAtual != null && quadraAtual.getId().equals(req.quadraId())
                    ? quadraAtual
                    : quadraRepository.buscarCompleta(req.quadraId())
                            .orElseThrow(() -> new RecursoNaoEncontradoException("Quadra não encontrada."));
            if (req.localId() != null && !req.localId().equals(quadra.getLocal().getId())) {
                throw new RequisicaoInvalidaException("A quadra informada não fica no local informado.");
            }
            if (req.esporteId() != null && !req.esporteId().equals(quadra.getEsporte().getId())) {
                throw new RequisicaoInvalidaException("A quadra informada é de outro esporte ("
                        + quadra.getEsporte().getNome() + ").");
            }
            return new Onde(quadra.getLocal(), quadra.getEsporte(), quadra);
        }
        if (req.localId() == null || req.esporteId() == null) {
            throw new RequisicaoInvalidaException(
                    "Informe a quadra (quadraId) ou o local e o esporte (localId e esporteId).");
        }
        Local local = localRepository.findById(req.localId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Local não encontrado."));
        Esporte esporte = esporteRepository.findById(req.esporteId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Esporte não encontrado."));
        return new Onde(local, esporte, null);
    }

    private static void aplicarOnde(Partida partida, Onde onde) {
        partida.setLocal(onde.local());
        partida.setEsporte(onde.esporte());
        partida.setQuadra(onde.quadra());
    }

    private static void aplicarTextos(Partida partida, PartidaRequest req) {
        partida.setTitulo(AuthService.textoOuNulo(req.titulo()));
        partida.setDescricao(AuthService.textoOuNulo(req.descricao()));
        partida.setImagemUrl(AuthService.textoOuNulo(req.imagemUrl()));
    }

    private void verificarCriador(Partida partida, UUID usuarioId) {
        if (!partida.isCriadaPor(usuarioId)) {
            throw new AcessoNegadoException("Apenas quem criou a partida pode fazer esta alteração.");
        }
    }

    private void verificarQuadraLivre(UUID quadraId, LocalDateTime inicio, LocalDateTime fim, UUID ignorarId) {
        // UUID aleatório quando não há partida a ignorar (nunca coincide com uma existente)
        UUID ignorar = ignorarId != null ? ignorarId : UUID.randomUUID();
        if (partidaRepository.contarConflitosDeHorario(quadraId, inicio, fim, ignorar, StatusPartida.CANCELADA) > 0) {
            throw new ConflitoException("Esta quadra já tem uma partida marcada nesse horário.");
        }
    }

    private void validarHorario(LocalDateTime inicio, LocalDateTime fim) {
        LocalDateTime agora = agora();
        if (!inicio.isAfter(agora)) {
            throw new RegraNegocioException("A partida deve começar no futuro.");
        }
        if (inicio.isAfter(agora.plusDays(ANTECEDENCIA_MAXIMA_DIAS))) {
            throw new RegraNegocioException("A partida pode ser marcada com no máximo 1 ano de antecedência.");
        }
        if (!fim.isAfter(inicio)) {
            throw new RequisicaoInvalidaException("dataHoraFim deve ser depois de dataHoraInicio.");
        }
        Duration duracao = Duration.between(inicio, fim);
        if (duracao.toMinutes() < DURACAO_MINIMA_MINUTOS) {
            throw new RegraNegocioException("A partida deve durar pelo menos " + DURACAO_MINIMA_MINUTOS + " minutos.");
        }
        if (duracao.toMinutes() > DURACAO_MAXIMA_HORAS * 60L) {
            throw new RegraNegocioException("A partida pode durar no máximo " + DURACAO_MAXIMA_HORAS + " horas.");
        }
    }

    static void validarCoordenadas(Double latitude, Double longitude) {
        if ((latitude == null) != (longitude == null)) {
            throw new RequisicaoInvalidaException("Informe latitude e longitude juntas (ou nenhuma das duas).");
        }
        if (latitude != null && (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180)) {
            throw new RequisicaoInvalidaException("Coordenadas fora do intervalo válido.");
        }
    }

    private void validarFiltro(PartidaFiltro f) {
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

    private long contarConfirmados(UUID partidaId) {
        return participanteRepository.contar(partidaId, StatusConfirmacao.CONFIRMADO);
    }

    private Map<UUID, Long> contarConfirmados(Collection<UUID> partidaIds) {
        Map<UUID, Long> mapa = new HashMap<>();
        if (partidaIds.isEmpty()) {
            return mapa;
        }
        for (Object[] linha : participanteRepository.contarPorPartidas(partidaIds, StatusConfirmacao.CONFIRMADO)) {
            mapa.put((UUID) linha[0], ((Number) linha[1]).longValue());
        }
        return mapa;
    }

    private boolean estaConfirmado(UUID partidaId, UUID usuarioId) {
        return participanteRepository.findById(new ParticipanteId(partidaId, usuarioId))
                .map(PartidaParticipante::isConfirmado)
                .orElse(false);
    }

    private Set<UUID> idsPartidasDoUsuario(UUID usuarioId) {
        if (usuarioId == null) {
            return Set.of();
        }
        return new HashSet<>(participanteRepository.buscarIdsPartidasDoUsuario(usuarioId,
                StatusConfirmacao.CONFIRMADO));
    }

    /** participa == null significa "visitante não logado" (campo omitido no JSON). */
    List<PartidaResponse> paraResponses(List<Partida> partidas, Double lat, Double lon, Set<UUID> participa) {
        Map<UUID, Long> confirmados = contarConfirmados(partidas.stream().map(Partida::getId).toList());
        return partidas.stream()
                .map(p -> paraResponse(p, confirmados.getOrDefault(p.getId(), 0L), lat, lon,
                        participa == null ? null : participa.contains(p.getId())))
                .toList();
    }

    private PartidaResponse paraResponse(Partida p, long confirmados, Double lat, Double lon, Boolean participa) {
        Quadra quadra = p.getQuadra();
        Local local = p.getLocal();
        Double distancia = null;
        if (lat != null && lon != null && local.temPosicao()) {
            distancia = GeoUtils.arredondar(distancia(p, lat, lon));
        }
        Long vagas = p.temLimite() ? Math.max(0, p.getMaxJogadores() - confirmados) : null;
        return new PartidaResponse(
                p.getId(),
                p.getTitulo(),
                p.getDescricao(),
                p.getImagemUrl(),
                EsporteResponse.de(p.getEsporte()),
                quadra == null ? null
                        : new PartidaResponse.QuadraInfo(quadra.getId(), quadra.getNome(), quadra.getPrecoHora()),
                LocalResumo.de(local),
                UsuarioResumo.de(p.getCriador()),
                p.getDataHoraInicio(),
                p.getDataHoraFim(),
                Duration.between(p.getDataHoraInicio(), p.getDataHoraFim()).toMinutes(),
                p.getMaxJogadores(),
                confirmados,
                vagas,
                p.getStatus(),
                p.calcularSituacao(agora(), confirmados),
                distancia,
                participa);
    }

    private static double distancia(Partida p, double lat, double lon) {
        Local local = p.getLocal();
        return GeoUtils.distanciaKm(lat, lon, local.getLatitude(), local.getLongitude());
    }

    private static double distanciaOuInfinito(Partida p, double lat, double lon) {
        return p.getLocal().temPosicao() ? distancia(p, lat, lon) : Double.MAX_VALUE;
    }
}
