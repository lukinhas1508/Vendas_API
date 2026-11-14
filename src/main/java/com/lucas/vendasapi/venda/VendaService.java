package com.lucas.vendasapi.venda;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lucas.vendasapi.exception.RegraNegocioException;
import com.lucas.vendasapi.pedido.PedidoRepository;
import com.lucas.vendasapi.pedido.StatusPedido;

/** Relatórios de vendas. Uma venda é um pedido com status PAGO, datada pelo pagamento. */
@Service
@Transactional(readOnly = true)
public class VendaService {

    private static final int LIMITE_MAXIMO = 100;

    private final PedidoRepository pedidoRepository;
    private final VendaRepository vendaRepository;

    public VendaService(PedidoRepository pedidoRepository, VendaRepository vendaRepository) {
        this.pedidoRepository = pedidoRepository;
        this.vendaRepository = vendaRepository;
    }

    public ResumoVendasResponse resumo(LocalDate inicio, LocalDate fim) {
        validarPeriodo(inicio, fim);
        LocalDateTime de = inicio.atStartOfDay();
        LocalDateTime ate = fim.plusDays(1).atStartOfDay(); // inclui o dia final inteiro

        long quantidade = pedidoRepository.contarPorStatusNoPeriodo(StatusPedido.PAGO, de, ate);
        BigDecimal soma = pedidoRepository.somarValorPorStatusNoPeriodo(StatusPedido.PAGO, de, ate);
        BigDecimal total = (soma == null ? BigDecimal.ZERO : soma).setScale(2, RoundingMode.HALF_UP);
        BigDecimal ticketMedio = quantidade == 0
                ? BigDecimal.ZERO.setScale(2)
                : total.divide(BigDecimal.valueOf(quantidade), 2, RoundingMode.HALF_UP);

        return new ResumoVendasResponse(inicio, fim, quantidade, total, ticketMedio);
    }

    public List<ProdutoMaisVendidoResponse> produtosMaisVendidos(LocalDate inicio, LocalDate fim, int limite) {
        validarPeriodo(inicio, fim);
        int tamanho = Math.max(1, Math.min(limite, LIMITE_MAXIMO));

        return vendaRepository.buscarProdutosMaisVendidos(StatusPedido.PAGO,
                        inicio.atStartOfDay(), fim.plusDays(1).atStartOfDay(), PageRequest.of(0, tamanho))
                .stream()
                .map(ProdutoMaisVendidoResponse::from)
                .toList();
    }

    private static void validarPeriodo(LocalDate inicio, LocalDate fim) {
        if (inicio.isAfter(fim)) {
            throw new RegraNegocioException("A data inicial não pode ser posterior à data final.");
        }
    }
}
