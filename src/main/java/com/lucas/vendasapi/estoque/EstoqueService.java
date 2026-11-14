package com.lucas.vendasapi.estoque;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lucas.vendasapi.exception.RecursoNaoEncontradoException;
import com.lucas.vendasapi.exception.RegraNegocioException;
import com.lucas.vendasapi.produto.Produto;
import com.lucas.vendasapi.produto.ProdutoRepository;

/**
 * Único ponto do sistema que altera o estoque. Toda alteração gera uma
 * movimentação, então o saldo do produto sempre pode ser explicado pelo histórico.
 */
@Service
public class EstoqueService {

    private final ProdutoRepository produtoRepository;
    private final MovimentacaoEstoqueRepository movimentacaoRepository;

    public EstoqueService(ProdutoRepository produtoRepository,
                          MovimentacaoEstoqueRepository movimentacaoRepository) {
        this.produtoRepository = produtoRepository;
        this.movimentacaoRepository = movimentacaoRepository;
    }

    /** Entrada manual de estoque (ex.: chegada de mercadoria), chamada pela API. */
    @Transactional
    public MovimentacaoResponse darEntrada(Long produtoId, EntradaEstoqueRequest request) {
        Produto produto = produtoRepository.findById(produtoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto", produtoId));
        if (!produto.isAtivo()) {
            throw new RegraNegocioException("Não é possível dar entrada em um produto inativo.");
        }
        return MovimentacaoResponse.from(registrarEntrada(produto, request.quantidade(), request.motivo()));
    }

    @Transactional
    public MovimentacaoEstoque registrarEntrada(Produto produto, int quantidade, String motivo) {
        produto.adicionarEstoque(quantidade);
        return registrar(produto, TipoMovimentacao.ENTRADA, quantidade, motivo);
    }

    /** Lança EstoqueInsuficienteException se não houver saldo. */
    @Transactional
    public MovimentacaoEstoque registrarSaida(Produto produto, int quantidade, String motivo) {
        produto.baixarEstoque(quantidade);
        return registrar(produto, TipoMovimentacao.SAIDA, quantidade, motivo);
    }

    @Transactional(readOnly = true)
    public Page<MovimentacaoResponse> listarMovimentacoes(Long produtoId, Pageable pageable) {
        if (!produtoRepository.existsById(produtoId)) {
            throw new RecursoNaoEncontradoException("Produto", produtoId);
        }
        return movimentacaoRepository.findByProdutoId(produtoId, pageable).map(MovimentacaoResponse::from);
    }

    private MovimentacaoEstoque registrar(Produto produto, TipoMovimentacao tipo, int quantidade, String motivo) {
        MovimentacaoEstoque movimentacao = new MovimentacaoEstoque(produto, tipo, quantidade, motivo);
        movimentacaoRepository.save(movimentacao);
        return movimentacao;
    }
}
