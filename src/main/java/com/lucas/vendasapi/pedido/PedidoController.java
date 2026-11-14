package com.lucas.vendasapi.pedido;

import java.net.URI;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/pedidos")
@Tag(name = "Pedidos")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @PostMapping
    @Operation(summary = "Cria um pedido e baixa o estoque dos itens")
    public ResponseEntity<PedidoResponse> criar(@Valid @RequestBody PedidoRequest request) {
        PedidoResponse pedido = pedidoService.criar(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(pedido.id()).toUri();
        return ResponseEntity.created(location).body(pedido);
    }

    @GetMapping
    @Operation(summary = "Lista pedidos (paginado), com filtro opcional por status")
    public Page<PedidoResponse> listar(
            @RequestParam(required = false) StatusPedido status,
            @ParameterObject @PageableDefault(size = 20, sort = "criadoEm", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return pedidoService.listar(status, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um pedido pelo id")
    public PedidoResponse buscarPorId(@PathVariable Long id) {
        return pedidoService.buscarPorId(id);
    }

    @PostMapping("/{id}/pagamento")
    @Operation(summary = "Confirma o pagamento do pedido")
    public PedidoResponse pagar(@PathVariable Long id) {
        return pedidoService.pagar(id);
    }

    @PostMapping("/{id}/cancelamento")
    @Operation(summary = "Cancela um pedido não pago e devolve os itens ao estoque")
    public PedidoResponse cancelar(@PathVariable Long id) {
        return pedidoService.cancelar(id);
    }
}
