package com.lucas.vendasapi.exception;

/** Recurso buscado por id não existe. Vira HTTP 404. */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String recurso, Long id) {
        super("%s com id %d não encontrado".formatted(recurso, id));
    }
}
