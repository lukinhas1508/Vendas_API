package com.lucas.vendasapi.produto;

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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.lucas.vendasapi.estoque.EstoqueService;
import com.lucas.vendasapi.estoque.MovimentacaoEstoque;
import com.lucas.vendasapi.estoque.MovimentacaoEstoqueRepository;
import com.lucas.vendasapi.exception.RecursoNaoEncontradoException;

@ExtendWith(MockitoExtension.class)
class ProdutoServiceTest {

    @Mock
    private ProdutoRepository produtoRepository;
    @Mock
    private MovimentacaoEstoqueRepository movimentacaoRepository;

    private ProdutoService produtoService;

    @BeforeEach
    void setUp() {
        EstoqueService estoqueService = new EstoqueService(produtoRepository, movimentacaoRepository);
        produtoService = new ProdutoService(produtoRepository, estoqueService);
    }

    @Test
    @DisplayName("criar: com estoque inicial, registra uma entrada de estoque")
    void criarProdutoComEstoqueInicial() {
        when(produtoRepository.save(any(Produto.class))).thenAnswer(chamada -> chamada.getArgument(0));

        ProdutoResponse produto = produtoService.criar(
                new ProdutoRequest("Teclado", "Mecânico", new BigDecimal("199.90"), 5));

        assertThat(produto.nome()).isEqualTo("Teclado");
        assertThat(produto.quantidadeEstoque()).isEqualTo(5);
        assertThat(produto.ativo()).isTrue();
        verify(movimentacaoRepository).save(any(MovimentacaoEstoque.class));
    }

    @Test
    @DisplayName("criar: sem estoque inicial, começa com zero e não gera movimentação")
    void criarProdutoSemEstoqueInicial() {
        when(produtoRepository.save(any(Produto.class))).thenAnswer(chamada -> chamada.getArgument(0));

        ProdutoResponse produto = produtoService.criar(
                new ProdutoRequest("Teclado", null, new BigDecimal("199.90"), null));

        assertThat(produto.quantidadeEstoque()).isZero();
        verify(movimentacaoRepository, never()).save(any(MovimentacaoEstoque.class));
    }

    @Test
    @DisplayName("listar: sem filtro de nome, busca todos os produtos ativos")
    void listarProdutosAtivos() {
        Pageable pageable = PageRequest.of(0, 20);
        Produto teclado = new Produto("Teclado", null, new BigDecimal("199.90"));
        when(produtoRepository.findByAtivoTrue(pageable)).thenReturn(new PageImpl<>(List.of(teclado)));

        Page<ProdutoResponse> pagina = produtoService.listar(" ", pageable);

        assertThat(pagina.getContent()).extracting(ProdutoResponse::nome).containsExactly("Teclado");
    }

    @Test
    @DisplayName("atualizar: muda nome, descrição e preço, mas nunca o estoque")
    void atualizarProduto() {
        Produto teclado = new Produto("Teclado", null, new BigDecimal("199.90"));
        teclado.adicionarEstoque(3);
        when(produtoRepository.findById(1L)).thenReturn(Optional.of(teclado));

        ProdutoResponse produto = produtoService.atualizar(1L,
                new ProdutoRequest("Teclado Gamer", "RGB", new BigDecimal("249.90"), 999));

        assertThat(produto.nome()).isEqualTo("Teclado Gamer");
        assertThat(produto.preco()).isEqualByComparingTo("249.90");
        assertThat(produto.quantidadeEstoque()).isEqualTo(3);
    }

    @Test
    @DisplayName("desativar: faz exclusão lógica do produto")
    void desativarProduto() {
        Produto teclado = new Produto("Teclado", null, new BigDecimal("199.90"));
        when(produtoRepository.findById(1L)).thenReturn(Optional.of(teclado));

        produtoService.desativar(1L);

        assertThat(teclado.isAtivo()).isFalse();
        verify(produtoRepository, never()).delete(any(Produto.class));
    }

    @Test
    @DisplayName("buscarPorId: id inexistente gera RecursoNaoEncontradoException")
    void buscarProdutoInexistente() {
        when(produtoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> produtoService.buscarPorId(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
