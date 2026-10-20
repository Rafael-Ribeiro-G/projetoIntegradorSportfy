package br.senai.sportfy.domain.enums;

/**
 * Coluna locais.tipo (texto em minúsculas no banco).
 * QUADRA = lugar com quadras cadastradas (clube, ginásio); os outros não precisam de quadra.
 */
public enum TipoLocal {
    QUADRA,
    PARQUE,
    ACADEMIA,
    PISTA,
    RUA,
    OUTRO;

    public String valorNoBanco() {
        return name().toLowerCase();
    }

    public static TipoLocal doBanco(String valor) {
        if (valor == null) {
            return OUTRO;
        }
        for (TipoLocal t : values()) {
            if (t.valorNoBanco().equalsIgnoreCase(valor.trim())) {
                return t;
            }
        }
        return OUTRO;
    }
}
