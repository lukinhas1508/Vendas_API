package com.lucas.vendasapi.venda;

import java.math.BigDecimal;

/** Projeção do Spring Data: cada getter corresponde a um alias da consulta JPQL. */
public interface ProdutoMaisVendido {

    Long getProdutoId();

    String getNome();

    Long getQuantidadeVendida();

    BigDecimal getTotalVendido();
}
