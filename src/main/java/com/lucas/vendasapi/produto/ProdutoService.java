package com.lucas.vendasapi.produto;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lucas.vendasapi.estoque.EstoqueService;
import com.lucas.vendasapi.exception.RecursoNaoEncontradoException;

@Service
public class ProdutoService {

    private final ProdutoRepository produtoRepository;
    private final EstoqueService estoqueService;

    public ProdutoService(ProdutoRepository produtoRepository, EstoqueService estoqueService) {
        this.produtoRepository = produtoRepository;
        this.estoqueService = estoqueService;
    }

    @Transactional
    public ProdutoResponse criar(ProdutoRequest request) {
        Produto produto = produtoRepository.save(
                new Produto(request.nome().trim(), request.descricao(), request.preco()));

        if (request.estoqueInicial() != null && request.estoqueInicial() > 0) {
            estoqueService.registrarEntrada(produto, request.estoqueInicial(), "Estoque inicial");
        }
        return ProdutoResponse.from(produto);
    }

    /** Lista apenas produtos ativos, com filtro opcional por parte do nome. */
    @Transactional(readOnly = true)
    public Page<ProdutoResponse> listar(String nome, Pageable pageable) {
        Page<Produto> produtos = (nome == null || nome.isBlank())
                ? produtoRepository.findByAtivoTrue(pageable)
                : produtoRepository.findByAtivoTrueAndNomeContainingIgnoreCase(nome.trim(), pageable);
        return produtos.map(ProdutoResponse::from);
    }

    @Transactional(readOnly = true)
    public ProdutoResponse buscarPorId(Long id) {
        return ProdutoResponse.from(buscarEntidade(id));
    }

    @Transactional
    public ProdutoResponse atualizar(Long id, ProdutoRequest request) {
        Produto produto = buscarEntidade(id);
        produto.atualizar(request.nome().trim(), request.descricao(), request.preco());
        return ProdutoResponse.from(produto);
    }

    /**
     * Exclusão lógica: o produto some das listagens e não pode mais ser vendido,
     * mas continua no banco para preservar o histórico dos pedidos.
     */
    @Transactional
    public void desativar(Long id) {
        buscarEntidade(id).desativar();
    }

    private Produto buscarEntidade(Long id) {
        return produtoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto", id));
    }
}
