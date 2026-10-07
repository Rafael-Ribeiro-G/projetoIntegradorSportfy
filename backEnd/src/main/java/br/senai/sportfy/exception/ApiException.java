package br.senai.sportfy.exception;

import org.springframework.http.HttpStatus;

/** Exceção base: carrega o status HTTP que deve ser devolvido ao cliente. */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;

    protected ApiException(HttpStatus status, String mensagem) {
        super(mensagem);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
