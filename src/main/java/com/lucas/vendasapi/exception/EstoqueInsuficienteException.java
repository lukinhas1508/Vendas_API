package com.lucas.vendasapi.exception;

public class EstoqueInsuficienteException extends RegraNegocioException {

    public EstoqueInsuficienteException(String produto, int disponivel, int solicitado) {
        super("Estoque insuficiente para o produto '%s': disponível %d, solicitado %d"
                .formatted(produto, disponivel, solicitado));
    }
}
