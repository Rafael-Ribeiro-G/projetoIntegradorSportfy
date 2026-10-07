package br.senai.sportfy.exception;

import org.springframework.http.HttpStatus;

/** 401 - e-mail ou senha incorretos. Mensagem genérica de propósito. */
public class CredenciaisInvalidasException extends ApiException {

    public CredenciaisInvalidasException() {
        super(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos.");
    }
}
