package com.lucas.vendasapi.cliente;

import java.time.LocalDateTime;

public record ClienteResponse(
        Long id, String nome, String email, String cpf, String telefone, LocalDateTime criadoEm) {

    public static ClienteResponse from(Cliente cliente) {
        return new ClienteResponse(cliente.getId(), cliente.getNome(), cliente.getEmail(),
                cliente.getCpf(), cliente.getTelefone(), cliente.getCriadoEm());
    }
}
