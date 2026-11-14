package com.lucas.vendasapi.produto;

import java.math.BigDecimal;

import com.lucas.vendasapi.exception.EstoqueInsuficienteException;
import com.lucas.vendasapi.exception.RegraNegocioException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "produtos")
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(length = 500)
    private String descricao;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal preco;

    @Column(name = "quantidade_estoque", nullable = false)
    private int quantidadeEstoque;

    @Column(nullable = false)
    private boolean ativo;

    /**
     * Lock otimista: se dois pedidos tentarem baixar o estoque do mesmo produto
     * ao mesmo tempo, o segundo falha (HTTP 409) em vez de vender estoque que não existe.
     */
    @Version
    private Long versao;

    protected Produto() {
        // exigido pelo JPA
    }

    public Produto(String nome, String descricao, BigDecimal preco) {
        this.nome = nome;
        this.descricao = descricao;
        this.preco = preco;
        this.quantidadeEstoque = 0;
        this.ativo = true;
    }

    public void atualizar(String nome, String descricao, BigDecimal preco) {
        this.nome = nome;
        this.descricao = descricao;
        this.preco = preco;
    }

    public void desativar() {
        this.ativo = false;
    }

    public void adicionarEstoque(int quantidade) {
        validarQuantidade(quantidade);
        this.quantidadeEstoque += quantidade;
    }

    public void baixarEstoque(int quantidade) {
        validarQuantidade(quantidade);
        if (quantidade > quantidadeEstoque) {
            throw new EstoqueInsuficienteException(nome, quantidadeEstoque, quantidade);
        }
        this.quantidadeEstoque -= quantidade;
    }

    private static void validarQuantidade(int quantidade) {
        if (quantidade <= 0) {
            throw new RegraNegocioException("A quantidade deve ser maior que zero.");
        }
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public int getQuantidadeEstoque() {
        return quantidadeEstoque;
    }

    public boolean isAtivo() {
        return ativo;
    }
}
