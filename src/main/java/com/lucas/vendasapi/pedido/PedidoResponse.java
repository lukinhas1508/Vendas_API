package com.lucas.vendasapi.pedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PedidoResponse(
        Long id,
        Long clienteId,
        String clienteNome,
        StatusPedido status,
        BigDecimal valorTotal,
        LocalDateTime criadoEm,
        LocalDateTime pagoEm,
        List<ItemPedidoResponse> itens) {

    public static PedidoResponse from(Pedido pedido) {
        return new PedidoResponse(
                pedido.getId(),
                pedido.getCliente().getId(),
                pedido.getCliente().getNome(),
                pedido.getStatus(),
                pedido.getValorTotal(),
                pedido.getCriadoEm(),
                pedido.getPagoEm(),
                pedido.getItens().stream().map(ItemPedidoResponse::from).toList());
    }
}
