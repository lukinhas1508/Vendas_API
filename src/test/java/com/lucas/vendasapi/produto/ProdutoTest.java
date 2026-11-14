package com.lucas.vendasapi.produto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.lucas.vendasapi.exception.EstoqueInsuficienteException;
import com.lucas.vendasapi.exception.RegraNegocioException;

/** Regras de estoque que vivem na própria entidade: teste puro, sem mocks. */
class ProdutoTest {

    private Produto produto;

    @BeforeEach
    void setUp() {
        produto = new Produto("Monitor", null, new BigDecimal("899.00"));
        produto.adicionarEstoque(10);
    }

    @Test
    @DisplayName("produto novo começa ativo")
    void produtoNovoComecaAtivo() {
        assertThat(produto.isAtivo()).isTrue();
    }

    @Test
    @DisplayName("baixarEstoque: diminui o saldo")
    void baixarEstoque() {
        produto.baixarEstoque(4);

        assertThat(produto.getQuantidadeEstoque()).isEqualTo(6);
    }

    @Test
    @DisplayName("baixarEstoque: permite zerar o saldo")
    void baixarTodoOEstoque() {
        produto.baixarEstoque(10);

        assertThat(produto.getQuantidadeEstoque()).isZero();
    }

    @Test
    @DisplayName("baixarEstoque: acima do saldo lança EstoqueInsuficienteException e mantém o saldo")
    void baixarEstoqueInsuficiente() {
        assertThatThrownBy(() -> produto.baixarEstoque(11))
                .isInstanceOf(EstoqueInsuficienteException.class)
                .hasMessageContaining("disponível 10")
                .hasMessageContaining("solicitado 11");

        assertThat(produto.getQuantidadeEstoque()).isEqualTo(10);
    }

    @Test
    @DisplayName("quantidade zero ou negativa é rejeitada na entrada e na saída")
    void quantidadeInvalida() {
        assertThatThrownBy(() -> produto.baixarEstoque(0)).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> produto.adicionarEstoque(-1)).isInstanceOf(RegraNegocioException.class);
    }
}
