package br.senai.sportfy.domain.enums;

/**
 * Status gravado na coluna partidas.status (texto em minúsculas no banco: "aberta", "cancelada", "finalizada").
 */
public enum StatusPartida {
    ABERTA,
    CANCELADA,
    FINALIZADA;

    public String valorNoBanco() {
        return name().toLowerCase();
    }

    /** Valores desconhecidos ou vazios são tratados como "aberta" (o padrão da tabela). */
    public static StatusPartida doBanco(String valor) {
        if (valor == null) {
            return ABERTA;
        }
        for (StatusPartida s : values()) {
            if (s.valorNoBanco().equalsIgnoreCase(valor.trim())) {
                return s;
            }
        }
        return ABERTA;
    }
}
