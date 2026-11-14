package com.lucas.vendasapi.venda;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/vendas")
@Tag(name = "Vendas", description = "Relatórios sobre pedidos pagos (somente ADMIN)")
public class VendaController {

    private final VendaService vendaService;

    public VendaController(VendaService vendaService) {
        this.vendaService = vendaService;
    }

    @GetMapping("/resumo")
    @Operation(summary = "Quantidade de vendas, valor total e ticket médio no período (datas em AAAA-MM-DD)")
    public ResumoVendasResponse resumo(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return vendaService.resumo(inicio, fim);
    }

    @GetMapping("/produtos-mais-vendidos")
    @Operation(summary = "Ranking dos produtos mais vendidos no período")
    public List<ProdutoMaisVendidoResponse> produtosMaisVendidos(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(defaultValue = "10") int limite) {
        return vendaService.produtosMaisVendidos(inicio, fim, limite);
    }
}
