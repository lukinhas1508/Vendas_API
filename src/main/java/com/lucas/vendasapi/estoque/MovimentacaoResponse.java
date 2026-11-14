package com.lucas.vendasapi.estoque;

import java.time.LocalDateTime;

public record MovimentacaoResponse(
        Long id, Long produtoId, TipoMovimentacao tipo, int quantidade, String motivo, LocalDateTime criadoEm) {

    public static MovimentacaoResponse from(MovimentacaoEstoque movimentacao) {
        return new MovimentacaoResponse(movimentacao.getId(), movimentacao.getProduto().getId(),
                movimentacao.getTipo(), movimentacao.getQuantidade(), movimentacao.getMotivo(),
                movimentacao.getCriadoEm());
    }
}
