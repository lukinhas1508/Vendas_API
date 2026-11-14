package com.lucas.vendasapi.venda;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ResumoVendasResponse(
        LocalDate inicio, LocalDate fim, long quantidadeVendas, BigDecimal valorTotal, BigDecimal ticketMedio) {
}
