package br.senai.sportfy.domain;

import br.senai.sportfy.domain.enums.StatusConfirmacao;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

/**
 * Tabela "partida_participantes": quem vai em qual partida.
 * Ao sair da partida, a linha é apagada.
 */
@Entity
@Table(name = "partida_participantes")
public class PartidaParticipante {

    @EmbeddedId
    private ParticipanteId id;

    @MapsId("partidaId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "partida_id")
    private Partida partida;

    @MapsId("usuarioId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Convert(converter = StatusConfirmacaoConverter.class)
    @Column(name = "status_confirmacao", length = 50)
    private StatusConfirmacao statusConfirmacao = StatusConfirmacao.CONFIRMADO;

    protected PartidaParticipante() {
    }

    public PartidaParticipante(Partida partida, Usuario usuario) {
        this.id = new ParticipanteId(partida.getId(), usuario.getId());
        this.partida = partida;
        this.usuario = usuario;
        this.statusConfirmacao = StatusConfirmacao.CONFIRMADO;
    }

    public boolean isConfirmado() {
        return statusConfirmacao == StatusConfirmacao.CONFIRMADO;
    }

    public ParticipanteId getId() {
        return id;
    }

    public Partida getPartida() {
        return partida;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public StatusConfirmacao getStatusConfirmacao() {
        return statusConfirmacao;
    }

    public void setStatusConfirmacao(StatusConfirmacao statusConfirmacao) {
        this.statusConfirmacao = statusConfirmacao;
    }
}
