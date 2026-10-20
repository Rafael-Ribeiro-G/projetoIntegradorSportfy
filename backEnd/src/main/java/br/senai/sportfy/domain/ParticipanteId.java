package br.senai.sportfy.domain;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Chave composta de partida_participantes: (partida_id, usuario_id). */
@Embeddable
public class ParticipanteId implements Serializable {

    @Column(name = "partida_id")
    private UUID partidaId;

    @Column(name = "usuario_id")
    private UUID usuarioId;

    public ParticipanteId() {
    }

    public ParticipanteId(UUID partidaId, UUID usuarioId) {
        this.partidaId = partidaId;
        this.usuarioId = usuarioId;
    }

    public UUID getPartidaId() {
        return partidaId;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ParticipanteId outro)) {
            return false;
        }
        return Objects.equals(partidaId, outro.partidaId) && Objects.equals(usuarioId, outro.usuarioId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(partidaId, usuarioId);
    }
}
