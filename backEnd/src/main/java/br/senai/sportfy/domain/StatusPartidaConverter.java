package br.senai.sportfy.domain;

import br.senai.sportfy.domain.enums.StatusPartida;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
class StatusPartidaConverter implements AttributeConverter<StatusPartida, String> {

    @Override
    public String convertToDatabaseColumn(StatusPartida status) {
        return status == null ? null : status.valorNoBanco();
    }

    @Override
    public StatusPartida convertToEntityAttribute(String valor) {
        return StatusPartida.doBanco(valor);
    }
}
