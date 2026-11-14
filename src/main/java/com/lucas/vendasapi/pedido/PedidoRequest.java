package com.lucas.vendasapi.pedido;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record PedidoRequest(
        @NotNull Long clienteId,
        @NotEmpty(message = "o pedido deve ter pelo menos um item") @Valid List<ItemPedidoRequest> itens) {
}
