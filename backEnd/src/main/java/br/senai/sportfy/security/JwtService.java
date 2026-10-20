package br.senai.sportfy.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import br.senai.sportfy.config.SportfyProperties;
import br.senai.sportfy.domain.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Gera e valida os tokens JWT (assinatura HMAC-SHA).
 * O segredo vem da variável de ambiente JWT_SECRET e nunca deve ir para o GitHub.
 */
@Service
public class JwtService {

    private static final int TAMANHO_MINIMO_SEGREDO = 32;
    private static final String CLAIM_VERSAO_SENHA = "sv";

    private final SecretKey chave;
    private final long expiracaoSegundos;
    private final String emissor;

    public JwtService(SportfyProperties properties) {
        String segredo = properties.jwt().secret();
        if (segredo == null || segredo.length() < TAMANHO_MINIMO_SEGREDO) {
            throw new IllegalStateException(
                    "Defina a variável de ambiente JWT_SECRET com pelo menos 32 caracteres "
                            + "(sugestão: openssl rand -base64 64).");
        }
        this.chave = Keys.hmacShaKeyFor(segredo.getBytes(StandardCharsets.UTF_8));
        this.expiracaoSegundos = properties.jwt().expiracaoMinutos() * 60;
        this.emissor = properties.jwt().emissor() == null ? "sportfy-api" : properties.jwt().emissor();
    }

    public String gerarToken(Usuario usuario) {
        Instant agora = Instant.now();
        return Jwts.builder()
                .issuer(emissor)
                .subject(String.valueOf(usuario.getId()))
                .claim("nome", usuario.getNome())
                .claim(CLAIM_VERSAO_SENHA, versaoSenha(usuario))
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plusSeconds(expiracaoSegundos)))
                .signWith(chave)
                .compact();
    }

    /** Dados lidos de um token válido (assinatura, emissor e validade conferidos). */
    public record TokenInfo(UUID usuarioId, String versaoSenha) {
    }

    public Optional<TokenInfo> validar(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(chave)
                    .requireIssuer(emissor)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(new TokenInfo(UUID.fromString(claims.getSubject()),
                    claims.get(CLAIM_VERSAO_SENHA, String.class)));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public Optional<UUID> extrairUsuarioId(String token) {
        return validar(token).map(TokenInfo::usuarioId);
    }

    /**
     * "Impressão digital" curta do hash da senha. Vai dentro do token: quando a senha muda,
     * os tokens antigos deixam de valer (o filtro compara com a versão atual).
     * Não revela nada da senha: é um SHA-256 do hash BCrypt, cortado.
     */
    public String versaoSenha(Usuario usuario) {
        String hash = usuario.getSenhaHash() == null ? "" : usuario.getSenhaHash();
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(hash.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível na JVM", e);
        }
    }

    public long getExpiracaoSegundos() {
        return expiracaoSegundos;
    }
}
