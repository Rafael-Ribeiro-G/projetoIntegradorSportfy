package br.senai.sportfy.domain;

import br.senai.sportfy.domain.enums.StatusConfirmacao;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
class StatusConfirmacaoConverter implements AttributeConverter<StatusConfirmacao, String> {

    @Override
    public String convertToDatabaseColumn(StatusConfirmacao status) {
        return status == null ? null : status.valorNoBanco();
    }

    @Override
    public StatusConfirmacao convertToEntityAttribute(String valor) {
        return StatusConfirmacao.doBanco(valor);
    }
}
