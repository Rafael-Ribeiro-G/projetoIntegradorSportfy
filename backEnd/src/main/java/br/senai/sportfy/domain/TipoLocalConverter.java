package br.senai.sportfy.domain;

import br.senai.sportfy.domain.enums.TipoLocal;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
class TipoLocalConverter implements AttributeConverter<TipoLocal, String> {

    @Override
    public String convertToDatabaseColumn(TipoLocal tipo) {
        return (tipo == null ? TipoLocal.OUTRO : tipo).valorNoBanco();
    }

    @Override
    public TipoLocal convertToEntityAttribute(String valor) {
        return TipoLocal.doBanco(valor);
    }
}
