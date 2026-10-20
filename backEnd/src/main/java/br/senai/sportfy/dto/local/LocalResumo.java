package br.senai.sportfy.dto.local;

import java.util.UUID;

import br.senai.sportfy.domain.Local;

public record LocalResumo(UUID id, String nome, String endereco, Double latitude, Double longitude) {

    public static LocalResumo de(Local l) {
        return new LocalResumo(l.getId(), l.getNome(), l.getEndereco(), l.getLatitude(), l.getLongitude());
    }
}
