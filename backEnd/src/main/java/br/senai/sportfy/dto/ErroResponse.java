package br.senai.sportfy.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Formato padrão de erro devolvido por TODAS as rotas da API.
 *
 * <pre>
 * {
 *   "timestamp": "2026-10-07T12:00:00",
 *   "status": 400,
 *   "erro": "Bad Request",
 *   "mensagem": "Dados inválidos.",
 *   "caminho": "/api/eventos",
 *   "campos": [ { "campo": "titulo", "mensagem": "é obrigatório" } ]
 * }
 * </pre>
 */
public record ErroResponse(
        LocalDateTime timestamp,
        int status,
        String erro,
        String mensagem,
        String caminho,
        List<CampoErro> campos) {

    public record CampoErro(String campo, String mensagem) {
    }

    public static ErroResponse de(int status, String erro, String mensagem, String caminho) {
        return new ErroResponse(LocalDateTime.now(), status, erro, mensagem, caminho, null);
    }
}
