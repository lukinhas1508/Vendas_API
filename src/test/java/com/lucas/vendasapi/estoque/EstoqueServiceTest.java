package com.lucas.vendasapi.estoque;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lucas.vendasapi.exception.EstoqueInsuficienteException;
import com.lucas.vendasapi.exception.RecursoNaoEncontradoException;
import com.lucas.vendasapi.exception.RegraNegocioException;
import com.lucas.vendasapi.produto.Produto;
import com.lucas.vendasapi.produto.ProdutoRepository;

@ExtendWith(MockitoExtension.class)
class EstoqueServiceTest {

    @Mock
    private ProdutoRepository produtoRepository;
    @Mock
    private MovimentacaoEstoqueRepository movimentacaoRepository;

    @InjectMocks
    private EstoqueService estoqueService;

    @Test
    @DisplayName("darEntrada: soma ao saldo e grava uma movimentação de ENTRADA")
    void darEntrada() {
        Produto produto = produtoComEstoque(2);
        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto));

        MovimentacaoResponse movimentacao =
                estoqueService.darEntrada(1L, new EntradaEstoqueRequest(5, "Compra do fornecedor"));

        assertThat(produto.getQuantidadeEstoque()).isEqualTo(7);
        assertThat(movimentacao.tipo()).isEqualTo(TipoMovimentacao.ENTRADA);
        assertThat(movimentacao.quantidade()).isEqualTo(5);
        assertThat(movimentacao.motivo()).isEqualTo("Compra do fornecedor");
        verify(movimentacaoRepository).save(any(MovimentacaoEstoque.class));
    }

    @Test
    @DisplayName("darEntrada: produto inexistente gera RecursoNaoEncontradoException")
    void darEntradaProdutoInexistente() {
        when(produtoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> estoqueService.darEntrada(99L, new EntradaEstoqueRequest(5, "Compra")))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("darEntrada: produto inativo não recebe estoque")
    void darEntradaProdutoInativo() {
        Produto produto = produtoComEstoque(2);
        produto.desativar();
        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto));

        assertThatThrownBy(() -> estoqueService.darEntrada(1L, new EntradaEstoqueRequest(5, "Compra")))
                .isInstanceOf(RegraNegocioException.class);

        assertThat(produto.getQuantidadeEstoque()).isEqualTo(2);
    }

    @Test
    @DisplayName("registrarSaida: diminui o saldo e grava uma movimentação de SAIDA")
    void registrarSaida() {
        Produto produto = produtoComEstoque(10);

        estoqueService.registrarSaida(produto, 4, "Venda");

        ArgumentCaptor<MovimentacaoEstoque> captor = ArgumentCaptor.forClass(MovimentacaoEstoque.class);
        verify(movimentacaoRepository).save(captor.capture());
        assertThat(produto.getQuantidadeEstoque()).isEqualTo(6);
        assertThat(captor.getValue().getTipo()).isEqualTo(TipoMovimentacao.SAIDA);
        assertThat(captor.getValue().getQuantidade()).isEqualTo(4);
    }

    @Test
    @DisplayName("registrarSaida: sem saldo suficiente, não grava movimentação")
    void registrarSaidaSemSaldo() {
        Produto produto = produtoComEstoque(3);

        assertThatThrownBy(() -> estoqueService.registrarSaida(produto, 4, "Venda"))
                .isInstanceOf(EstoqueInsuficienteException.class);

        assertThat(produto.getQuantidadeEstoque()).isEqualTo(3);
        verify(movimentacaoRepository, never()).save(any(MovimentacaoEstoque.class));
    }

    private static Produto produtoComEstoque(int quantidade) {
        Produto produto = new Produto("Mouse", null, new BigDecimal("59.90"));
        produto.adicionarEstoque(quantidade);
        return produto;
    }
}
