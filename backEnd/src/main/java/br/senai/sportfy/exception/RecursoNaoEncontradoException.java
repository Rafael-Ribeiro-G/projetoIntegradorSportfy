package br.senai.sportfy.exception;

import org.springframework.http.HttpStatus;

/** 404 - o recurso pedido não existe. */
public class RecursoNaoEncontradoException extends ApiException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(HttpStatus.NOT_FOUND, mensagem);
    }
}
