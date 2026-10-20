package br.senai.sportfy.domain;

import java.time.LocalDateTime;
import java.util.UUID;

import br.senai.sportfy.domain.enums.SituacaoPartida;
import br.senai.sportfy.domain.enums.StatusPartida;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Tabela "partidas": uma atividade esportiva marcada num local, num horário
 * (pelada na quadra, corrida no parque, treino na academia...).
 * A quadra é opcional; quando existe, o local e o esporte são os dela.
 * max_jogadores vazio = sem limite de vagas.
 */
@Entity
@Table(name = "partidas")
public class Partida {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "criador_id", nullable = false)
    private Usuario criador;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "local_id", nullable = false)
    private Local local;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "esporte_id", nullable = false)
    private Esporte esporte;

    /** Opcional: só para atividades numa quadra específica. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quadra_id")
    private Quadra quadra;

    @Column(length = 100)
    private String titulo;

    @Column(columnDefinition = "text")
    private String descricao;

    @Column(name = "imagem_url", columnDefinition = "text")
    private String imagemUrl;

    @Column(name = "data_hora_inicio", nullable = false)
    private LocalDateTime dataHoraInicio;

    @Column(name = "data_hora_fim", nullable = false)
    private LocalDateTime dataHoraFim;

    @Column(name = "max_jogadores")
    private Integer maxJogadores;

    @Convert(converter = StatusPartidaConverter.class)
    @Column(length = 50)
    private StatusPartida status = StatusPartida.ABERTA;

    // ------------------------------------------------------------ regras

    public boolean isCancelada() {
        return status == StatusPartida.CANCELADA;
    }

    public boolean jaComecou(LocalDateTime agora) {
        return !agora.isBefore(dataHoraInicio);
    }

    public boolean jaTerminou(LocalDateTime agora) {
        return !agora.isBefore(dataHoraFim);
    }

    public boolean isCriadaPor(UUID usuarioId) {
        return criador != null && criador.getId().equals(usuarioId);
    }

    public boolean temLimite() {
        return maxJogadores != null;
    }

    public boolean estaLotada(long confirmados) {
        return temLimite() && confirmados >= maxJogadores;
    }

    public SituacaoPartida calcularSituacao(LocalDateTime agora, long confirmados) {
        if (isCancelada()) {
            return SituacaoPartida.CANCELADA;
        }
        if (status == StatusPartida.FINALIZADA || jaTerminou(agora)) {
            return SituacaoPartida.ENCERRADA;
        }
        if (jaComecou(agora)) {
            return SituacaoPartida.EM_ANDAMENTO;
        }
        return estaLotada(confirmados) ? SituacaoPartida.LOTADA : SituacaoPartida.ABERTA;
    }

    // ------------------------------------------------------------ get/set

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Usuario getCriador() {
        return criador;
    }

    public void setCriador(Usuario criador) {
        this.criador = criador;
    }

    public Local getLocal() {
        return local;
    }

    public void setLocal(Local local) {
        this.local = local;
    }

    public Esporte getEsporte() {
        return esporte;
    }

    public void setEsporte(Esporte esporte) {
        this.esporte = esporte;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getImagemUrl() {
        return imagemUrl;
    }

    public void setImagemUrl(String imagemUrl) {
        this.imagemUrl = imagemUrl;
    }

    public Quadra getQuadra() {
        return quadra;
    }

    public void setQuadra(Quadra quadra) {
        this.quadra = quadra;
    }

    public LocalDateTime getDataHoraInicio() {
        return dataHoraInicio;
    }

    public void setDataHoraInicio(LocalDateTime dataHoraInicio) {
        this.dataHoraInicio = dataHoraInicio;
    }

    public LocalDateTime getDataHoraFim() {
        return dataHoraFim;
    }

    public void setDataHoraFim(LocalDateTime dataHoraFim) {
        this.dataHoraFim = dataHoraFim;
    }

    public Integer getMaxJogadores() {
        return maxJogadores;
    }

    public void setMaxJogadores(Integer maxJogadores) {
        this.maxJogadores = maxJogadores;
    }

    public StatusPartida getStatus() {
        return status;
    }

    public void setStatus(StatusPartida status) {
        this.status = status;
    }
}
