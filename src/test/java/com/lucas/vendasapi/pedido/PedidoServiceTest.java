package com.lucas.vendasapi.pedido;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lucas.vendasapi.cliente.Cliente;
import com.lucas.vendasapi.cliente.ClienteRepository;
import com.lucas.vendasapi.estoque.EstoqueService;
import com.lucas.vendasapi.estoque.MovimentacaoEstoque;
import com.lucas.vendasapi.estoque.MovimentacaoEstoqueRepository;
import com.lucas.vendasapi.exception.EstoqueInsuficienteException;
import com.lucas.vendasapi.exception.RecursoNaoEncontradoException;
import com.lucas.vendasapi.exception.RegraNegocioException;
import com.lucas.vendasapi.produto.Produto;
import com.lucas.vendasapi.produto.ProdutoRepository;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    private static final Long CLIENTE_ID = 1L;
    private static final Long PRODUTO_ID = 10L;
    private static final Long PEDIDO_ID = 100L;

    @Mock
    private PedidoRepository pedidoRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private ProdutoRepository produtoRepository;
    @Mock
    private MovimentacaoEstoqueRepository movimentacaoRepository;

    private PedidoService pedidoService;
    private Cliente cliente;

    @BeforeEach
    void setUp() {
        // EstoqueService real (só o repositório é mock) para testar a baixa de estoque de verdade
        EstoqueService estoqueService = new EstoqueService(produtoRepository, movimentacaoRepository);
        pedidoService = new PedidoService(pedidoRepository, clienteRepository, produtoRepository, estoqueService);
        cliente = new Cliente("Ana Souza", "ana@email.com", "52998224725", "11999990000");
    }

    @Test
    @DisplayName("criar: calcula o total, baixa o estoque e registra a saída")
    void criarPedidoComSucesso() {
        Produto teclado = produtoComEstoque("Teclado", "50.00", 10);
        when(clienteRepository.findById(CLIENTE_ID)).thenReturn(Optional.of(cliente));
        when(produtoRepository.findById(PRODUTO_ID)).thenReturn(Optional.of(teclado));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(chamada -> chamada.getArgument(0));

        PedidoResponse pedido = pedidoService.criar(pedidoDe(PRODUTO_ID, 2));

        assertThat(pedido.status()).isEqualTo(StatusPedido.AGUARDANDO_PAGAMENTO);
        assertThat(pedido.valorTotal()).isEqualByComparingTo("100.00");
        assertThat(pedido.itens()).hasSize(1);
        assertThat(pedido.itens().get(0).subtotal()).isEqualByComparingTo("100.00");
        assertThat(teclado.getQuantidadeEstoque()).isEqualTo(8);
        verify(movimentacaoRepository).save(any(MovimentacaoEstoque.class));
    }

    @Test
    @DisplayName("criar: sem estoque suficiente, lança exceção e não salva o pedido")
    void criarPedidoSemEstoque() {
        Produto teclado = produtoComEstoque("Teclado", "50.00", 1);
        when(clienteRepository.findById(CLIENTE_ID)).thenReturn(Optional.of(cliente));
        when(produtoRepository.findById(PRODUTO_ID)).thenReturn(Optional.of(teclado));

        assertThatThrownBy(() -> pedidoService.criar(pedidoDe(PRODUTO_ID, 5)))
                .isInstanceOf(EstoqueInsuficienteException.class)
                .hasMessageContaining("Teclado");

        assertThat(teclado.getQuantidadeEstoque()).isEqualTo(1);
        verify(pedidoRepository, never()).save(any(Pedido.class));
    }

    @Test
    @DisplayName("criar: cliente inexistente gera RecursoNaoEncontradoException")
    void criarPedidoClienteInexistente() {
        when(clienteRepository.findById(CLIENTE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pedidoService.criar(pedidoDe(PRODUTO_ID, 1)))
                .isInstanceOf(RecursoNaoEncontradoException.class);

        verify(pedidoRepository, never()).save(any(Pedido.class));
    }

    @Test
    @DisplayName("criar: produto inativo não pode ser vendido")
    void criarPedidoProdutoInativo() {
        Produto teclado = produtoComEstoque("Teclado", "50.00", 10);
        teclado.desativar();
        when(clienteRepository.findById(CLIENTE_ID)).thenReturn(Optional.of(cliente));
        when(produtoRepository.findById(PRODUTO_ID)).thenReturn(Optional.of(teclado));

        assertThatThrownBy(() -> pedidoService.criar(pedidoDe(PRODUTO_ID, 1)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("inativo");

        assertThat(teclado.getQuantidadeEstoque()).isEqualTo(10);
    }

    @Test
    @DisplayName("pagar: muda o status para PAGO e registra a data do pagamento")
    void pagarPedido() {
        Pedido pedido = pedidoCom(produtoComEstoque("Mouse", "30.00", 5), 1);
        when(pedidoRepository.findById(PEDIDO_ID)).thenReturn(Optional.of(pedido));

        PedidoResponse resposta = pedidoService.pagar(PEDIDO_ID);

        assertThat(resposta.status()).isEqualTo(StatusPedido.PAGO);
        assertThat(resposta.pagoEm()).isNotNull();
    }

    @Test
    @DisplayName("cancelar: muda o status para CANCELADO e devolve os itens ao estoque")
    void cancelarPedido() {
        Produto mouse = produtoComEstoque("Mouse", "30.00", 7);
        Pedido pedido = pedidoCom(mouse, 3);
        when(pedidoRepository.findById(PEDIDO_ID)).thenReturn(Optional.of(pedido));

        PedidoResponse resposta = pedidoService.cancelar(PEDIDO_ID);

        assertThat(resposta.status()).isEqualTo(StatusPedido.CANCELADO);
        assertThat(mouse.getQuantidadeEstoque()).isEqualTo(10);
        verify(movimentacaoRepository).save(any(MovimentacaoEstoque.class));
    }

    @Test
    @DisplayName("cancelar: pedido já pago não pode ser cancelado")
    void cancelarPedidoPago() {
        Produto mouse = produtoComEstoque("Mouse", "30.00", 7);
        Pedido pedido = pedidoCom(mouse, 3);
        pedido.pagar();
        when(pedidoRepository.findById(PEDIDO_ID)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> pedidoService.cancelar(PEDIDO_ID))
                .isInstanceOf(RegraNegocioException.class);

        assertThat(mouse.getQuantidadeEstoque()).isEqualTo(7);
        verify(movimentacaoRepository, never()).save(any(MovimentacaoEstoque.class));
    }

    private Pedido pedidoCom(Produto produto, int quantidade) {
        Pedido pedido = new Pedido(cliente);
        pedido.adicionarItem(produto, quantidade);
        return pedido;
    }

    private static PedidoRequest pedidoDe(Long produtoId, int quantidade) {
        return new PedidoRequest(CLIENTE_ID, List.of(new ItemPedidoRequest(produtoId, quantidade)));
    }

    private static Produto produtoComEstoque(String nome, String preco, int estoque) {
        Produto produto = new Produto(nome, null, new BigDecimal(preco));
        produto.adicionarEstoque(estoque);
        return produto;
    }
}
