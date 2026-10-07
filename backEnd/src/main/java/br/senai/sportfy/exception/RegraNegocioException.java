package br.senai.sportfy.exception;

import org.springframework.http.HttpStatus;

/** 422 - os dados são válidos, mas violam uma regra de negócio (ex.: evento lotado). */
public class RegraNegocioException extends ApiException {

    public RegraNegocioException(String mensagem) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, mensagem);
    }
}
