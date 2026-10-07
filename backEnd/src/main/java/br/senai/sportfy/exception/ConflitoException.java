package br.senai.sportfy.exception;

import org.springframework.http.HttpStatus;

/** 409 - conflito com um dado que já existe (ex.: e-mail já cadastrado). */
public class ConflitoException extends ApiException {

    public ConflitoException(String mensagem) {
        super(HttpStatus.CONFLICT, mensagem);
    }
}
