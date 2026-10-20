package br.senai.sportfy.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configurações próprias da aplicação (prefixo "sportfy" no application.yml).
 */
@ConfigurationProperties(prefix = "sportfy")
public record SportfyProperties(Jwt jwt, Cors cors, Login login, Uploads uploads) {

    public record Jwt(String secret, long expiracaoMinutos, String emissor) {
    }

    public record Cors(String origensPermitidas) {
        public List<String> listaOrigens() {
            if (origensPermitidas == null || origensPermitidas.isBlank()) {
                return List.of();
            }
            return Arrays.stream(origensPermitidas.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }
    }

    public record Login(int maxTentativas, long bloqueioMinutos) {
    }

    /** Pasta onde ficam as imagens enviadas (fotos de perfil e capas de evento). */
    public record Uploads(String pasta) {
    }
}
