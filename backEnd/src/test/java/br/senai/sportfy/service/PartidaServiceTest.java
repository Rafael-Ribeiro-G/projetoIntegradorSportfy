package br.senai.sportfy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.senai.sportfy.domain.Esporte;
import br.senai.sportfy.domain.Local;
import br.senai.sportfy.domain.ParticipanteId;
import br.senai.sportfy.domain.Partida;
import br.senai.sportfy.domain.PartidaParticipante;
import br.senai.sportfy.domain.Quadra;
import br.senai.sportfy.domain.Usuario;
import br.senai.sportfy.domain.enums.StatusConfirmacao;
import br.senai.sportfy.domain.enums.StatusPartida;
import br.senai.sportfy.dto.partida.PartidaRequest;
import br.senai.sportfy.dto.partida.PartidaResponse;
import br.senai.sportfy.exception.AcessoNegadoException;
import br.senai.sportfy.exception.ConflitoException;
import br.senai.sportfy.exception.RegraNegocioException;
import br.senai.sportfy.exception.RequisicaoInvalidaException;
import br.senai.sportfy.repository.EsporteRepository;
import br.senai.sportfy.repository.LocalRepository;
import br.senai.sportfy.repository.PartidaParticipanteRepository;
import br.senai.sportfy.repository.PartidaRepository;
import br.senai.sportfy.repository.QuadraRepository;
import br.senai.sportfy.repository.UsuarioRepository;

/**
 * Testes unitários das regras de partidas (sem banco: repositórios simulados com Mockito).
 */
@ExtendWith(MockitoExtension.class)
class PartidaServiceTest {

    /** "Agora" fixo nos testes: 19/10/2026 12:00 (horário de Brasília). */
    private static final Clock RELOGIO = Clock.fixed(Instant.parse("2026-10-19T15:00:00Z"),
            ZoneId.of("America/Sao_Paulo"));
    private static final LocalDateTime AGORA = LocalDateTime.now(RELOGIO);

    private static final UUID PARTIDA_ID = UUID.fromString("00000000-0000-0000-0000-0000000000aa");

    @Mock
    private PartidaRepository partidaRepository;
    @Mock
    private PartidaParticipanteRepository participanteRepository;
    @Mock
    private QuadraRepository quadraRepository;
    @Mock
    private LocalRepository localRepository;
    @Mock
    private EsporteRepository esporteRepository;
    @Mock
    private UsuarioRepository usuarioRepository;

    private PartidaService service;

    private Usuario criador;
    private Usuario jogador;
    private Quadra quadra;

    @BeforeEach
    void setUp() {
        service = new PartidaService(partidaRepository, participanteRepository, quadraRepository, localRepository,
                esporteRepository, usuarioRepository, RELOGIO);
        criador = usuario("Ana");
        jogador = usuario("Bruno");

        Local local = new Local();
        local.setId(UUID.randomUUID());
        local.setNome("Clube da Vila");
        local.setEndereco("Rua A, 100 - São Paulo");
        local.definirPosicao(-23.55, -46.63);

        quadra = new Quadra();
        quadra.setId(UUID.randomUUID());
        quadra.setNome("Quadra 1");
        quadra.setLocal(local);
        quadra.setEsporte(new Esporte(2, "Futsal", 10));
        quadra.setPrecoHora(new BigDecimal("90.00"));
    }

    // ------------------------------------------------------------------ criar

    @Test
    void criar_criadorJaEntraComoParticipanteEUsaLimitePadraoDoEsporte() {
        when(quadraRepository.buscarCompleta(quadra.getId())).thenReturn(Optional.of(quadra));
        when(usuarioRepository.findById(criador.getId())).thenReturn(Optional.of(criador));
        when(partidaRepository.save(any(Partida.class))).thenAnswer(inv -> {
            Partida p = inv.getArgument(0);
            p.setId(PARTIDA_ID);
            return p;
        });

        PartidaResponse resposta = service.criar(request(null), criador.getId());

        assertThat(resposta.id()).isEqualTo(PARTIDA_ID);
        assertThat(resposta.maxJogadores()).isEqualTo(10);
        assertThat(resposta.participantesConfirmados()).isEqualTo(1);
        assertThat(resposta.vagasRestantes()).isEqualTo(9);
        assertThat(resposta.usuarioParticipa()).isTrue();
        assertThat(resposta.esporte().nome()).isEqualTo("Futsal");
        assertThat(resposta.duracaoMinutos()).isEqualTo(90);
        verify(participanteRepository).save(any(PartidaParticipante.class));
    }

    @Test
    void criar_quadraOcupadaNoHorario_lancaConflito() {
        when(quadraRepository.buscarCompleta(quadra.getId())).thenReturn(Optional.of(quadra));
        when(partidaRepository.contarConflitosDeHorario(eq(quadra.getId()), any(), any(), any(),
                eq(StatusPartida.CANCELADA))).thenReturn(1L);

        assertThatThrownBy(() -> service.criar(request(10), criador.getId()))
                .isInstanceOf(ConflitoException.class)
                .hasMessageContaining("horário");
        verify(partidaRepository, never()).save(any());
    }

    @Test
    void criar_fimAntesDoInicio_lancaRequisicaoInvalida() {
        PartidaRequest req = naQuadra(AGORA.plusDays(2), AGORA.plusDays(2).minusHours(1), 10);
        assertThatThrownBy(() -> service.criar(req, criador.getId())).isInstanceOf(RequisicaoInvalidaException.class);
    }

    @Test
    void criar_duracaoMuitoCurta_lancaRegraNegocio() {
        PartidaRequest req = naQuadra(AGORA.plusDays(2), AGORA.plusDays(2).plusMinutes(5), 10);
        assertThatThrownBy(() -> service.criar(req, criador.getId()))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("15 minutos");
    }

    // -------------------------------------------------------------- participar

    @Test
    void participar_comVaga_confirmaPresenca() {
        Partida partida = partida(StatusPartida.ABERTA, AGORA.plusDays(2), 10);
        when(partidaRepository.buscarParaAtualizacao(PARTIDA_ID)).thenReturn(Optional.of(partida));
        when(participanteRepository.findById(new ParticipanteId(PARTIDA_ID, jogador.getId()))).thenReturn(Optional.empty());
        when(participanteRepository.contar(PARTIDA_ID, StatusConfirmacao.CONFIRMADO)).thenReturn(3L);
        when(usuarioRepository.findById(jogador.getId())).thenReturn(Optional.of(jogador));

        PartidaResponse resposta = service.participar(PARTIDA_ID, jogador.getId());

        assertThat(resposta.participantesConfirmados()).isEqualTo(4);
        assertThat(resposta.usuarioParticipa()).isTrue();
        verify(participanteRepository).save(any(PartidaParticipante.class));
    }

    @Test
    void participar_partidaLotada_lancaRegraNegocio() {
        Partida partida = partida(StatusPartida.ABERTA, AGORA.plusDays(2), 2);
        when(partidaRepository.buscarParaAtualizacao(PARTIDA_ID)).thenReturn(Optional.of(partida));
        when(participanteRepository.findById(any())).thenReturn(Optional.empty());
        when(participanteRepository.contar(PARTIDA_ID, StatusConfirmacao.CONFIRMADO)).thenReturn(2L);

        assertThatThrownBy(() -> service.participar(PARTIDA_ID, jogador.getId()))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("lotada");
        verify(participanteRepository, never()).save(any());
    }

    @Test
    void participar_semLimite_sempreTemVaga() {
        Partida partida = partida(StatusPartida.ABERTA, AGORA.plusDays(2), null);
        when(partidaRepository.buscarParaAtualizacao(PARTIDA_ID)).thenReturn(Optional.of(partida));
        when(participanteRepository.findById(any())).thenReturn(Optional.empty());
        when(participanteRepository.contar(PARTIDA_ID, StatusConfirmacao.CONFIRMADO)).thenReturn(300L);
        when(usuarioRepository.findById(jogador.getId())).thenReturn(Optional.of(jogador));

        PartidaResponse resposta = service.participar(PARTIDA_ID, jogador.getId());

        assertThat(resposta.vagasRestantes()).isNull();
        assertThat(resposta.participantesConfirmados()).isEqualTo(301);
    }

    @Test
    void participar_jaConfirmado_lancaConflito() {
        Partida partida = partida(StatusPartida.ABERTA, AGORA.plusDays(2), 10);
        when(partidaRepository.buscarParaAtualizacao(PARTIDA_ID)).thenReturn(Optional.of(partida));
        when(participanteRepository.findById(any())).thenReturn(Optional.of(new PartidaParticipante(partida, jogador)));

        assertThatThrownBy(() -> service.participar(PARTIDA_ID, jogador.getId())).isInstanceOf(ConflitoException.class);
    }

    @Test
    void participar_registroPendente_viraConfirmado() {
        Partida partida = partida(StatusPartida.ABERTA, AGORA.plusDays(2), 10);
        PartidaParticipante pendente = new PartidaParticipante(partida, jogador);
        pendente.setStatusConfirmacao(StatusConfirmacao.PENDENTE);
        when(partidaRepository.buscarParaAtualizacao(PARTIDA_ID)).thenReturn(Optional.of(partida));
        when(participanteRepository.findById(any())).thenReturn(Optional.of(pendente));
        when(participanteRepository.contar(PARTIDA_ID, StatusConfirmacao.CONFIRMADO)).thenReturn(1L);

        service.participar(PARTIDA_ID, jogador.getId());

        assertThat(pendente.getStatusConfirmacao()).isEqualTo(StatusConfirmacao.CONFIRMADO);
        verify(participanteRepository, never()).save(any());
    }

    @Test
    void participar_partidaCancelada_lancaRegraNegocio() {
        Partida partida = partida(StatusPartida.CANCELADA, AGORA.plusDays(2), 10);
        when(partidaRepository.buscarParaAtualizacao(PARTIDA_ID)).thenReturn(Optional.of(partida));

        assertThatThrownBy(() -> service.participar(PARTIDA_ID, jogador.getId()))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("cancelada");
    }

    @Test
    void participar_partidaJaComecou_lancaRegraNegocio() {
        Partida partida = partida(StatusPartida.ABERTA, AGORA.minusMinutes(10), 10);
        when(partidaRepository.buscarParaAtualizacao(PARTIDA_ID)).thenReturn(Optional.of(partida));

        assertThatThrownBy(() -> service.participar(PARTIDA_ID, jogador.getId()))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("começou");
    }

    // -------------------------------------------------------------------- sair

    @Test
    void sair_criadorNaoPodeSair() {
        Partida partida = partida(StatusPartida.ABERTA, AGORA.plusDays(2), 10);
        when(partidaRepository.buscarParaAtualizacao(PARTIDA_ID)).thenReturn(Optional.of(partida));

        assertThatThrownBy(() -> service.sair(PARTIDA_ID, criador.getId())).isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void sair_participanteConfirmado_apagaParticipacao() {
        Partida partida = partida(StatusPartida.ABERTA, AGORA.plusDays(2), 10);
        PartidaParticipante participacao = new PartidaParticipante(partida, jogador);
        when(partidaRepository.buscarParaAtualizacao(PARTIDA_ID)).thenReturn(Optional.of(partida));
        when(participanteRepository.findById(any())).thenReturn(Optional.of(participacao));
        when(participanteRepository.contar(PARTIDA_ID, StatusConfirmacao.CONFIRMADO)).thenReturn(3L);

        PartidaResponse resposta = service.sair(PARTIDA_ID, jogador.getId());

        verify(participanteRepository).delete(participacao);
        assertThat(resposta.participantesConfirmados()).isEqualTo(2);
        assertThat(resposta.usuarioParticipa()).isFalse();
    }

    // ---------------------------------------------------------- editar/cancelar

    @Test
    void atualizar_quemNaoCriou_lancaAcessoNegado() {
        Partida partida = partida(StatusPartida.ABERTA, AGORA.plusDays(2), 10);
        when(partidaRepository.buscarParaAtualizacao(PARTIDA_ID)).thenReturn(Optional.of(partida));

        assertThatThrownBy(() -> service.atualizar(PARTIDA_ID, request(10), jogador.getId()))
                .isInstanceOf(AcessoNegadoException.class);
    }

    @Test
    void atualizar_limiteMenorQueConfirmados_lancaRegraNegocio() {
        Partida partida = partida(StatusPartida.ABERTA, AGORA.plusDays(2), 10);
        when(partidaRepository.buscarParaAtualizacao(PARTIDA_ID)).thenReturn(Optional.of(partida));
        when(participanteRepository.contar(PARTIDA_ID, StatusConfirmacao.CONFIRMADO)).thenReturn(5L);

        assertThatThrownBy(() -> service.atualizar(PARTIDA_ID, request(3), criador.getId()))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("5");
    }

    @Test
    void cancelar_quemNaoCriou_lancaAcessoNegado() {
        Partida partida = partida(StatusPartida.ABERTA, AGORA.plusDays(2), 10);
        when(partidaRepository.buscarParaAtualizacao(PARTIDA_ID)).thenReturn(Optional.of(partida));

        assertThatThrownBy(() -> service.cancelar(PARTIDA_ID, jogador.getId()))
                .isInstanceOf(AcessoNegadoException.class);
        assertThat(partida.getStatus()).isEqualTo(StatusPartida.ABERTA);
    }

    @Test
    void cancelar_criadorCancela() {
        Partida partida = partida(StatusPartida.ABERTA, AGORA.plusDays(2), 10);
        when(partidaRepository.buscarParaAtualizacao(PARTIDA_ID)).thenReturn(Optional.of(partida));

        service.cancelar(PARTIDA_ID, criador.getId());

        assertThat(partida.getStatus()).isEqualTo(StatusPartida.CANCELADA);
    }

    @Test
    void cancelar_partidaJaCancelada_lancaRegraNegocio() {
        Partida partida = partida(StatusPartida.CANCELADA, AGORA.plusDays(2), 10);
        when(partidaRepository.buscarParaAtualizacao(PARTIDA_ID)).thenReturn(Optional.of(partida));

        assertThatThrownBy(() -> service.cancelar(PARTIDA_ID, criador.getId()))
                .isInstanceOf(RegraNegocioException.class);
    }

    // ----------------------------------------------------------------- helpers

    private static Usuario usuario(String nome) {
        Usuario u = new Usuario();
        u.setId(UUID.randomUUID());
        u.setNome(nome);
        u.setEmail(nome.toLowerCase() + "@teste.com");
        return u;
    }

    private Partida partida(StatusPartida status, LocalDateTime inicio, Integer max) {
        Partida p = new Partida();
        p.setId(PARTIDA_ID);
        p.setCriador(criador);
        p.setQuadra(quadra);
        p.setLocal(quadra.getLocal());
        p.setEsporte(quadra.getEsporte());
        p.setDataHoraInicio(inicio);
        p.setDataHoraFim(inicio.plusMinutes(90));
        p.setMaxJogadores(max);
        p.setStatus(status);
        return p;
    }

    private PartidaRequest request(Integer max) {
        LocalDateTime inicio = AGORA.plusDays(3);
        return naQuadra(inicio, inicio.plusMinutes(90), max);
    }

    private PartidaRequest naQuadra(LocalDateTime inicio, LocalDateTime fim, Integer max) {
        return new PartidaRequest(null, quadra.getId(), null, "Pelada de quarta", null, null, inicio, fim, max);
    }

    // ------------------------------------------------------- sem quadra (novo)

    @Test
    void criar_semQuadra_usaLocalEEsporteInformados() {
        Local parque = quadra.getLocal();
        Esporte corrida = new Esporte(5, "Corrida", null);
        when(localRepository.findById(parque.getId())).thenReturn(Optional.of(parque));
        when(esporteRepository.findById(5)).thenReturn(Optional.of(corrida));
        when(usuarioRepository.findById(criador.getId())).thenReturn(Optional.of(criador));
        when(partidaRepository.save(any(Partida.class))).thenAnswer(inv -> {
            Partida p = inv.getArgument(0);
            p.setId(PARTIDA_ID);
            return p;
        });
        LocalDateTime inicio = AGORA.plusDays(1);
        PartidaRequest req = new PartidaRequest(parque.getId(), null, 5, "Corrida 5 km", "Ritmo leve",
                "https://exemplo.com/capa.jpg", inicio, inicio.plusHours(1), null);

        PartidaResponse resposta = service.criar(req, criador.getId());

        assertThat(resposta.quadra()).isNull();
        assertThat(resposta.esporte().nome()).isEqualTo("Corrida");
        assertThat(resposta.titulo()).isEqualTo("Corrida 5 km");
        assertThat(resposta.imagemUrl()).isEqualTo("https://exemplo.com/capa.jpg");
        assertThat(resposta.maxJogadores()).isNull();
        assertThat(resposta.vagasRestantes()).isNull();
        // sem quadra não há conflito de horário a checar
        verify(partidaRepository, never()).contarConflitosDeHorario(any(), any(), any(), any(), any());
    }

    @Test
    void criar_semQuadraESemLocal_lancaRequisicaoInvalida() {
        LocalDateTime inicio = AGORA.plusDays(1);
        PartidaRequest req = new PartidaRequest(null, null, 5, null, null, null, inicio, inicio.plusHours(1), null);
        assertThatThrownBy(() -> service.criar(req, criador.getId()))
                .isInstanceOf(RequisicaoInvalidaException.class)
                .hasMessageContaining("quadraId");
    }

    @Test
    void criar_quadraDeOutroEsporte_lancaRequisicaoInvalida() {
        when(quadraRepository.buscarCompleta(quadra.getId())).thenReturn(Optional.of(quadra));
        LocalDateTime inicio = AGORA.plusDays(1);
        PartidaRequest req = new PartidaRequest(null, quadra.getId(), 99, null, null, null, inicio,
                inicio.plusHours(1), 10);
        assertThatThrownBy(() -> service.criar(req, criador.getId()))
                .isInstanceOf(RequisicaoInvalidaException.class)
                .hasMessageContaining("Futsal");
    }
}
