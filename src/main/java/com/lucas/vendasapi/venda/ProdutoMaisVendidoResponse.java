package com.lucas.vendasapi.venda;

import java.math.BigDecimal;

public record ProdutoMaisVendidoResponse(
        Long produtoId, String nome, long quantidadeVendida, BigDecimal totalVendido) {

    public static ProdutoMaisVendidoResponse from(ProdutoMaisVendido projecao) {
        return new ProdutoMaisVendidoResponse(projecao.getProdutoId(), projecao.getNome(),
                projecao.getQuantidadeVendida(), projecao.getTotalVendido());
    }
}
