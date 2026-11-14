package com.lucas.vendasapi.exception;

/** A requisição é válida, mas viola uma regra de negócio. Vira HTTP 422. */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
