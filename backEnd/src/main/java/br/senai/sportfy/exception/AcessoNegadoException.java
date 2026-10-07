package br.senai.sportfy.exception;

import org.springframework.http.HttpStatus;

/** 403 - o usuário está logado, mas não tem permissão para esta ação. */
public class AcessoNegadoException extends ApiException {

    public AcessoNegadoException(String mensagem) {
        super(HttpStatus.FORBIDDEN, mensagem);
    }
}
