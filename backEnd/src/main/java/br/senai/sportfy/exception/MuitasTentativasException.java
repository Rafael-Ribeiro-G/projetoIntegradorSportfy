package br.senai.sportfy.exception;

import org.springframework.http.HttpStatus;

/** 429 - login bloqueado temporariamente após várias tentativas erradas. */
public class MuitasTentativasException extends ApiException {

    public MuitasTentativasException(long minutos) {
        super(HttpStatus.TOO_MANY_REQUESTS,
                "Muitas tentativas de login. Tente novamente em " + minutos + " minuto(s).");
    }
}
