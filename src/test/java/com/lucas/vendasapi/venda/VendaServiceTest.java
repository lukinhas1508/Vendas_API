package com.lucas.vendasapi.venda;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import com.lucas.vendasapi.exception.RegraNegocioException;
import com.lucas.vendasapi.pedido.PedidoRepository;
import com.lucas.vendasapi.pedido.StatusPedido;

@ExtendWith(MockitoExtension.class)
class VendaServiceTest {

    private static final LocalDate INICIO = LocalDate.of(2026, 11, 1);
    private static final LocalDate FIM = LocalDate.of(2026, 11, 30);
    // o período consultado vai do início do primeiro dia até o início do dia seguinte ao último
    private static final LocalDateTime DE = LocalDateTime.of(2026, 11, 1, 0, 0);
    private static final LocalDateTime ATE = LocalDateTime.of(2026, 12, 1, 0, 0);

    @Mock
    private PedidoRepository pedidoRepository;
    @Mock
    private VendaRepository vendaRepository;

    @InjectMocks
    private VendaService vendaService;

    @Test
    @DisplayName("resumo: calcula total e ticket médio dos pedidos pagos")
    void resumoComVendas() {
        when(pedidoRepository.contarPorStatusNoPeriodo(StatusPedido.PAGO, DE, ATE)).thenReturn(3L);
        when(pedidoRepository.somarValorPorStatusNoPeriodo(StatusPedido.PAGO, DE, ATE))
                .thenReturn(new BigDecimal("100.00"));

        ResumoVendasResponse resumo = vendaService.resumo(INICIO, FIM);

        assertThat(resumo.quantidadeVendas()).isEqualTo(3);
        assertThat(resumo.valorTotal()).isEqualByComparingTo("100.00");
        assertThat(resumo.ticketMedio()).isEqualByComparingTo("33.33");
    }

    @Test
    @DisplayName("resumo: período sem vendas devolve zeros (sem divisão por zero)")
    void resumoSemVendas() {
        when(pedidoRepository.contarPorStatusNoPeriodo(StatusPedido.PAGO, DE, ATE)).thenReturn(0L);
        when(pedidoRepository.somarValorPorStatusNoPeriodo(StatusPedido.PAGO, DE, ATE)).thenReturn(null);

        ResumoVendasResponse resumo = vendaService.resumo(INICIO, FIM);

        assertThat(resumo.quantidadeVendas()).isZero();
        assertThat(resumo.valorTotal()).isEqualByComparingTo("0");
        assertThat(resumo.ticketMedio()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("resumo: data inicial depois da final é rejeitada")
    void resumoPeriodoInvalido() {
        assertThatThrownBy(() -> vendaService.resumo(FIM, INICIO))
                .isInstanceOf(RegraNegocioException.class);

        verifyNoInteractions(pedidoRepository);
    }

    @Test
    @DisplayName("produtosMaisVendidos: converte a projeção e respeita o limite máximo de 100")
    void produtosMaisVendidos() {
        ProdutoMaisVendido projecao = mock(ProdutoMaisVendido.class);
        when(projecao.getProdutoId()).thenReturn(10L);
        when(projecao.getNome()).thenReturn("Teclado");
        when(projecao.getQuantidadeVendida()).thenReturn(7L);
        when(projecao.getTotalVendido()).thenReturn(new BigDecimal("350.00"));
        when(vendaRepository.buscarProdutosMaisVendidos(StatusPedido.PAGO, DE, ATE, PageRequest.of(0, 100)))
                .thenReturn(List.of(projecao));

        List<ProdutoMaisVendidoResponse> ranking = vendaService.produtosMaisVendidos(INICIO, FIM, 500);

        assertThat(ranking).containsExactly(
                new ProdutoMaisVendidoResponse(10L, "Teclado", 7L, new BigDecimal("350.00")));
    }
}
