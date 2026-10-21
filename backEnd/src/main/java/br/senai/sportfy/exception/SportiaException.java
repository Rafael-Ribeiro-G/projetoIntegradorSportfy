package br.senai.sportfy.exception;

import org.springframework.http.HttpStatus;

/** Falhas do chatbot SportIA: 429 (limite), 502 (resposta vazia), 503 (fora do ar), 504 (demorou). */
public class SportiaException extends ApiException {

    public SportiaException(HttpStatus status, String mensagem) {
        super(status, mensagem);
    }
}
