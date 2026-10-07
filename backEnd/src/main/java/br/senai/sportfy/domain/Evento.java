package br.senai.sportfy.domain;

import java.time.LocalDateTime;

import br.senai.sportfy.domain.enums.NivelHabilidade;
import br.senai.sportfy.domain.enums.SituacaoEvento;
import br.senai.sportfy.domain.enums.StatusEvento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "eventos")
public class Evento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String titulo;

    @Column(length = 1000)
    private String descricao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "esporte_id", nullable = false)
    private Esporte esporte;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organizador_id", nullable = false)
    private Usuario organizador;

    @Column(name = "data_hora_inicio", nullable = false)
    private LocalDateTime dataHoraInicio;

    @Column(name = "duracao_minutos", nullable = false)
    private Integer duracaoMinutos;

    @Column(name = "local_nome", nullable = false, length = 150)
    private String localNome;

    @Column(length = 255)
    private String endereco;

    @Column(nullable = false, length = 100)
    private String cidade;

    private Double latitude;

    private Double longitude;

    @Column(name = "max_participantes", nullable = false)
    private Integer maxParticipantes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NivelHabilidade nivel = NivelHabilidade.TODOS;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusEvento status = StatusEvento.ATIVO;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @PrePersist
    void antesDeInserir() {
        LocalDateTime agora = LocalDateTime.now();
        this.criadoEm = agora;
        this.atualizadoEm = agora;
    }

    @PreUpdate
    void antesDeAtualizar() {
        this.atualizadoEm = LocalDateTime.now();
    }

    // ----------------------------------------------------------------- regras

    public LocalDateTime getDataHoraFim() {
        return dataHoraInicio.plusMinutes(duracaoMinutos);
    }

    public boolean isCancelado() {
        return status == StatusEvento.CANCELADO;
    }

    public boolean jaComecou(LocalDateTime agora) {
        return !agora.isBefore(dataHoraInicio);
    }

    public boolean isOrganizadoPor(Long usuarioId) {
        return organizador != null && organizador.getId() != null && organizador.getId().equals(usuarioId);
    }

    /** Calcula a situação atual do evento para exibição. */
    public SituacaoEvento calcularSituacao(LocalDateTime agora, long participantesConfirmados) {
        if (isCancelado()) {
            return SituacaoEvento.CANCELADO;
        }
        if (!agora.isBefore(getDataHoraFim())) {
            return SituacaoEvento.ENCERRADO;
        }
        if (jaComecou(agora)) {
            return SituacaoEvento.EM_ANDAMENTO;
        }
        if (participantesConfirmados >= maxParticipantes) {
            return SituacaoEvento.LOTADO;
        }
        return SituacaoEvento.ABERTO;
    }

    // -------------------------------------------------------- getters/setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Esporte getEsporte() {
        return esporte;
    }

    public void setEsporte(Esporte esporte) {
        this.esporte = esporte;
    }

    public Usuario getOrganizador() {
        return organizador;
    }

    public void setOrganizador(Usuario organizador) {
        this.organizador = organizador;
    }

    public LocalDateTime getDataHoraInicio() {
        return dataHoraInicio;
    }

    public void setDataHoraInicio(LocalDateTime dataHoraInicio) {
        this.dataHoraInicio = dataHoraInicio;
    }

    public Integer getDuracaoMinutos() {
        return duracaoMinutos;
    }

    public void setDuracaoMinutos(Integer duracaoMinutos) {
        this.duracaoMinutos = duracaoMinutos;
    }

    public String getLocalNome() {
        return localNome;
    }

    public void setLocalNome(String localNome) {
        this.localNome = localNome;
    }

    public String getEndereco() {
        return endereco;
    }

    public void setEndereco(String endereco) {
        this.endereco = endereco;
    }

    public String getCidade() {
        return cidade;
    }

    public void setCidade(String cidade) {
        this.cidade = cidade;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Integer getMaxParticipantes() {
        return maxParticipantes;
    }

    public void setMaxParticipantes(Integer maxParticipantes) {
        this.maxParticipantes = maxParticipantes;
    }

    public NivelHabilidade getNivel() {
        return nivel;
    }

    public void setNivel(NivelHabilidade nivel) {
        this.nivel = nivel;
    }

    public StatusEvento getStatus() {
        return status;
    }

    public void setStatus(StatusEvento status) {
        this.status = status;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }
}
