package com.lucas.vendasapi.pedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.lucas.vendasapi.cliente.Cliente;
import com.lucas.vendasapi.exception.RegraNegocioException;
import com.lucas.vendasapi.produto.Produto;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/**
 * Ciclo de vida: AGUARDANDO_PAGAMENTO -> PAGO (vira uma venda)
 *                AGUARDANDO_PAGAMENTO -> CANCELADO (estoque devolvido)
 */
@Entity
@Table(name = "pedidos")
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusPedido status;

    @Column(name = "valor_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal valorTotal;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "pago_em")
    private LocalDateTime pagoEm;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemPedido> itens = new ArrayList<>();

    protected Pedido() {
        // exigido pelo JPA
    }

    public Pedido(Cliente cliente) {
        this.cliente = cliente;
        this.status = StatusPedido.AGUARDANDO_PAGAMENTO;
        this.valorTotal = BigDecimal.ZERO;
        this.criadoEm = LocalDateTime.now();
    }

    public void adicionarItem(Produto produto, int quantidade) {
        ItemPedido item = new ItemPedido(this, produto, quantidade);
        itens.add(item);
        valorTotal = valorTotal.add(item.getSubtotal());
    }

    public void pagar() {
        exigirAguardandoPagamento("pago");
        this.status = StatusPedido.PAGO;
        this.pagoEm = LocalDateTime.now();
    }

    public void cancelar() {
        exigirAguardandoPagamento("cancelado");
        this.status = StatusPedido.CANCELADO;
    }

    private void exigirAguardandoPagamento(String acao) {
        if (status != StatusPedido.AGUARDANDO_PAGAMENTO) {
            throw new RegraNegocioException(
                    "Pedido com status %s não pode ser %s.".formatted(status, acao));
        }
    }

    public Long getId() {
        return id;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public StatusPedido getStatus() {
        return status;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public LocalDateTime getPagoEm() {
        return pagoEm;
    }

    public List<ItemPedido> getItens() {
        return Collections.unmodifiableList(itens);
    }
}
