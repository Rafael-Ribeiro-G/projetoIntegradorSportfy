package br.senai.sportfy.exception;

import org.springframework.http.HttpStatus;

/** 400 - parâmetros da requisição inválidos ou incoerentes entre si. */
public class RequisicaoInvalidaException extends ApiException {

    public RequisicaoInvalidaException(String mensagem) {
        super(HttpStatus.BAD_REQUEST, mensagem);
    }
}
