package com.lucas.vendasapi.pedido;

import java.math.BigDecimal;

public record ItemPedidoResponse(
        Long produtoId, String produtoNome, int quantidade, BigDecimal precoUnitario, BigDecimal subtotal) {

    public static ItemPedidoResponse from(ItemPedido item) {
        return new ItemPedidoResponse(item.getProduto().getId(), item.getProduto().getNome(),
                item.getQuantidade(), item.getPrecoUnitario(), item.getSubtotal());
    }
}
