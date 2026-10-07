package br.senai.sportfy.dto.auth;

import br.senai.sportfy.dto.usuario.UsuarioResponse;

/**
 * Resposta de login/cadastro.
 * O front/mobile deve enviar o token em todas as rotas protegidas:
 * {@code Authorization: Bearer <token>}
 */
public record TokenResponse(
        String token,
        String tipo,
        long expiraEmSegundos,
        UsuarioResponse usuario) {
}
