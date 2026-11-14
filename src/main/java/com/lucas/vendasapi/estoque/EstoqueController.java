package com.lucas.vendasapi.estoque;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/produtos/{produtoId}/estoque")
@Tag(name = "Estoque")
public class EstoqueController {

    private final EstoqueService estoqueService;

    public EstoqueController(EstoqueService estoqueService) {
        this.estoqueService = estoqueService;
    }

    @PostMapping("/entradas")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra uma entrada de estoque para o produto")
    public MovimentacaoResponse darEntrada(@PathVariable Long produtoId,
                                           @Valid @RequestBody EntradaEstoqueRequest request) {
        return estoqueService.darEntrada(produtoId, request);
    }

    @GetMapping("/movimentacoes")
    @Operation(summary = "Lista o histórico de entradas e saídas do produto (paginado)")
    public Page<MovimentacaoResponse> listarMovimentacoes(
            @PathVariable Long produtoId,
            @ParameterObject @PageableDefault(size = 20, sort = "criadoEm", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return estoqueService.listarMovimentacoes(produtoId, pageable);
    }
}
