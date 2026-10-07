package br.senai.sportfy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.senai.sportfy.domain.Esporte;
import br.senai.sportfy.domain.Evento;
import br.senai.sportfy.domain.Participacao;
import br.senai.sportfy.domain.Usuario;
import br.senai.sportfy.domain.enums.NivelHabilidade;
import br.senai.sportfy.domain.enums.Perfil;
import br.senai.sportfy.domain.enums.StatusEvento;
import br.senai.sportfy.domain.enums.StatusParticipacao;
import br.senai.sportfy.dto.evento.EventoRequest;
import br.senai.sportfy.dto.evento.EventoResponse;
import br.senai.sportfy.exception.AcessoNegadoException;
import br.senai.sportfy.exception.ConflitoException;
import br.senai.sportfy.exception.RegraNegocioException;
import br.senai.sportfy.exception.RequisicaoInvalidaException;
import br.senai.sportfy.repository.EsporteRepository;
import br.senai.sportfy.repository.EventoRepository;
import br.senai.sportfy.repository.ParticipacaoRepository;
import br.senai.sportfy.repository.UsuarioRepository;
import br.senai.sportfy.security.UsuarioAutenticado;

/**
 * Testes unitários das regras de negócio de eventos (sem banco: repositórios simulados com Mockito).
 */
@ExtendWith(MockitoExtension.class)
class EventoServiceTest {

    /** "Agora" fixo nos testes: 07/10/2026 12:00 (horário de Brasília). */
    private static final Clock RELOGIO = Clock.fixed(Instant.parse("2026-10-07T15:00:00Z"),
            ZoneId.of("America/Sao_Paulo"));
    private static final LocalDateTime AGORA = LocalDateTime.now(RELOGIO);

    @Mock
    private EventoRepository eventoRepository;
    @Mock
    private ParticipacaoRepository participacaoRepository;
    @Mock
    private EsporteRepository esporteRepository;
    @Mock
    private UsuarioRepository usuarioRepository;

    private EventoService service;

    private Usuario organizador;
    private Usuario jogador;
    private Esporte futebol;

    @BeforeEach
    void setUp() {
        service = new EventoService(eventoRepository, participacaoRepository, esporteRepository,
                usuarioRepository, RELOGIO);
        organizador = usuario(1L, "Ana");
        jogador = usuario(2L, "Bruno");
        futebol = new Esporte("Futebol", "Futebol de campo", 22);
        futebol.setId(5L);
    }

    // ------------------------------------------------------------------ criar

    @Test
    void criar_organizadorJaEntraComoParticipante() {
        when(esporteRepository.findById(5L)).thenReturn(Optional.of(futebol));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(organizador));
        when(eventoRepository.save(any(Evento.class))).thenAnswer(inv -> {
            Evento e = inv.getArgument(0);
            e.setId(100L);
            return e;
        });

        EventoResponse resposta = service.criar(request(10, null, null), 1L);

        assertThat(resposta.id()).isEqualTo(100L);
        assertThat(resposta.participantesConfirmados()).isEqualTo(1);
        assertThat(resposta.vagasRestantes()).isEqualTo(9);
        assertThat(resposta.usuarioParticipa()).isTrue();
        assertThat(resposta.organizador().id()).isEqualTo(1L);
        verify(participacaoRepository).save(any(Participacao.class));
    }

    @Test
    void criar_comLatitudeSemLongitude_lancaRequisicaoInvalida() {
        assertThatThrownBy(() -> service.criar(request(10, -23.5, null), 1L))
                .isInstanceOf(RequisicaoInvalidaException.class);
        verify(eventoRepository, never()).save(any());
    }

    @Test
    void criar_esporteInativo_lancaRegraNegocio() {
        futebol.setAtivo(false);
        when(esporteRepository.findById(5L)).thenReturn(Optional.of(futebol));

        assertThatThrownBy(() -> service.criar(request(10, null, null), 1L))
                .isInstanceOf(RegraNegocioException.class);
    }

    // -------------------------------------------------------------- participar

    @Test
    void participar_comVaga_confirmaPresenca() {
        Evento evento = evento(StatusEvento.ATIVO, AGORA.plusDays(2), 10);
        when(eventoRepository.buscarParaAtualizacao(100L)).thenReturn(Optional.of(evento));
        when(participacaoRepository.findByEventoIdAndUsuarioId(100L, 2L)).thenReturn(Optional.empty());
        when(participacaoRepository.countByEventoIdAndStatus(100L, StatusParticipacao.CONFIRMADO)).thenReturn(3L);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(jogador));

        EventoResponse resposta = service.participar(100L, 2L);

        assertThat(resposta.participantesConfirmados()).isEqualTo(4);
        assertThat(resposta.usuarioParticipa()).isTrue();
        verify(participacaoRepository).save(any(Participacao.class));
    }

    @Test
    void participar_eventoLotado_lancaRegraNegocio() {
        Evento evento = evento(StatusEvento.ATIVO, AGORA.plusDays(2), 2);
        when(eventoRepository.buscarParaAtualizacao(100L)).thenReturn(Optional.of(evento));
        when(participacaoRepository.findByEventoIdAndUsuarioId(100L, 2L)).thenReturn(Optional.empty());
        when(participacaoRepository.countByEventoIdAndStatus(100L, StatusParticipacao.CONFIRMADO)).thenReturn(2L);

        assertThatThrownBy(() -> service.participar(100L, 2L))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("lotado");
        verify(participacaoRepository, never()).save(any());
    }

    @Test
    void participar_jaConfirmado_lancaConflito() {
        Evento evento = evento(StatusEvento.ATIVO, AGORA.plusDays(2), 10);
        when(eventoRepository.buscarParaAtualizacao(100L)).thenReturn(Optional.of(evento));
        when(participacaoRepository.findByEventoIdAndUsuarioId(100L, 2L))
                .thenReturn(Optional.of(new Participacao(evento, jogador)));

        assertThatThrownBy(() -> service.participar(100L, 2L)).isInstanceOf(ConflitoException.class);
    }

    @Test
    void participar_quemTinhaSaido_reativaParticipacao() {
        Evento evento = evento(StatusEvento.ATIVO, AGORA.plusDays(2), 10);
        Participacao antiga = new Participacao(evento, jogador);
        antiga.setStatus(StatusParticipacao.CANCELADO);
        when(eventoRepository.buscarParaAtualizacao(100L)).thenReturn(Optional.of(evento));
        when(participacaoRepository.findByEventoIdAndUsuarioId(100L, 2L)).thenReturn(Optional.of(antiga));
        when(participacaoRepository.countByEventoIdAndStatus(100L, StatusParticipacao.CONFIRMADO)).thenReturn(1L);

        service.participar(100L, 2L);

        assertThat(antiga.getStatus()).isEqualTo(StatusParticipacao.CONFIRMADO);
        verify(participacaoRepository, never()).save(any());
    }

    @Test
    void participar_eventoCancelado_lancaRegraNegocio() {
        Evento evento = evento(StatusEvento.CANCELADO, AGORA.plusDays(2), 10);
        when(eventoRepository.buscarParaAtualizacao(100L)).thenReturn(Optional.of(evento));

        assertThatThrownBy(() -> service.participar(100L, 2L))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("cancelado");
    }

    @Test
    void participar_eventoJaComecou_lancaRegraNegocio() {
        Evento evento = evento(StatusEvento.ATIVO, AGORA.minusMinutes(10), 10);
        when(eventoRepository.buscarParaAtualizacao(100L)).thenReturn(Optional.of(evento));

        assertThatThrownBy(() -> service.participar(100L, 2L))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("começou");
    }

    // -------------------------------------------------------------------- sair

    @Test
    void sair_organizadorNaoPodeSair() {
        Evento evento = evento(StatusEvento.ATIVO, AGORA.plusDays(2), 10);
        when(eventoRepository.buscarParaAtualizacao(100L)).thenReturn(Optional.of(evento));

        assertThatThrownBy(() -> service.sair(100L, 1L)).isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void sair_participanteConfirmado_cancelaParticipacao() {
        Evento evento = evento(StatusEvento.ATIVO, AGORA.plusDays(2), 10);
        Participacao participacao = new Participacao(evento, jogador);
        when(eventoRepository.buscarParaAtualizacao(100L)).thenReturn(Optional.of(evento));
        when(participacaoRepository.findByEventoIdAndUsuarioId(100L, 2L)).thenReturn(Optional.of(participacao));
        when(participacaoRepository.countByEventoIdAndStatus(100L, StatusParticipacao.CONFIRMADO)).thenReturn(3L);

        EventoResponse resposta = service.sair(100L, 2L);

        assertThat(participacao.getStatus()).isEqualTo(StatusParticipacao.CANCELADO);
        assertThat(resposta.participantesConfirmados()).isEqualTo(2);
        assertThat(resposta.usuarioParticipa()).isFalse();
    }

    // ---------------------------------------------------------- editar/cancelar

    @Test
    void atualizar_quemNaoEOrganizador_lancaAcessoNegado() {
        Evento evento = evento(StatusEvento.ATIVO, AGORA.plusDays(2), 10);
        when(eventoRepository.buscarParaAtualizacao(100L)).thenReturn(Optional.of(evento));
        UsuarioAutenticado outro = new UsuarioAutenticado(2L, "bruno@teste.com", Perfil.USER);

        assertThatThrownBy(() -> service.atualizar(100L, request(10, null, null), outro))
                .isInstanceOf(AcessoNegadoException.class);
    }

    @Test
    void atualizar_limiteMenorQueConfirmados_lancaRegraNegocio() {
        Evento evento = evento(StatusEvento.ATIVO, AGORA.plusDays(2), 10);
        when(eventoRepository.buscarParaAtualizacao(100L)).thenReturn(Optional.of(evento));
        when(participacaoRepository.countByEventoIdAndStatus(100L, StatusParticipacao.CONFIRMADO)).thenReturn(5L);
        UsuarioAutenticado dono = new UsuarioAutenticado(1L, "ana@teste.com", Perfil.USER);

        assertThatThrownBy(() -> service.atualizar(100L, request(3, null, null), dono))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("5");
    }

    @Test
    void cancelar_adminPodeCancelarEventoDeOutro() {
        Evento evento = evento(StatusEvento.ATIVO, AGORA.plusDays(2), 10);
        when(eventoRepository.buscarParaAtualizacao(100L)).thenReturn(Optional.of(evento));
        UsuarioAutenticado admin = new UsuarioAutenticado(99L, "admin@teste.com", Perfil.ADMIN);

        service.cancelar(100L, admin);

        assertThat(evento.getStatus()).isEqualTo(StatusEvento.CANCELADO);
    }

    @Test
    void cancelar_eventoJaCancelado_lancaRegraNegocio() {
        Evento evento = evento(StatusEvento.CANCELADO, AGORA.plusDays(2), 10);
        when(eventoRepository.buscarParaAtualizacao(100L)).thenReturn(Optional.of(evento));
        UsuarioAutenticado dono = new UsuarioAutenticado(1L, "ana@teste.com", Perfil.USER);

        assertThatThrownBy(() -> service.cancelar(100L, dono)).isInstanceOf(RegraNegocioException.class);
    }

    // ----------------------------------------------------------------- helpers

    private static Usuario usuario(Long id, String nome) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setNome(nome);
        u.setEmail(nome.toLowerCase() + "@teste.com");
        u.setPerfil(Perfil.USER);
        return u;
    }

    private Evento evento(StatusEvento status, LocalDateTime inicio, int max) {
        Evento e = new Evento();
        e.setId(100L);
        e.setTitulo("Pelada de quarta");
        e.setEsporte(futebol);
        e.setOrganizador(organizador);
        e.setDataHoraInicio(inicio);
        e.setDuracaoMinutos(90);
        e.setLocalNome("Quadra do bairro");
        e.setCidade("São Paulo");
        e.setMaxParticipantes(max);
        e.setNivel(NivelHabilidade.TODOS);
        e.setStatus(status);
        return e;
    }

    private static EventoRequest request(int max, Double lat, Double lon) {
        return new EventoRequest("Pelada de quarta", "Traga água", 5L, AGORA.plusDays(3), 90,
                "Quadra do bairro", "Rua A, 100", "São Paulo", lat, lon, max, NivelHabilidade.TODOS);
    }
}
