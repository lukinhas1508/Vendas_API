package com.lucas.vendasapi.pedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    Page<Pedido> findByStatus(StatusPedido status, Pageable pageable);

    boolean existsByClienteId(Long clienteId);

    @Query("""
            select count(p) from Pedido p
            where p.status = :status and p.pagoEm >= :inicio and p.pagoEm < :fim
            """)
    long contarPorStatusNoPeriodo(@Param("status") StatusPedido status,
                                  @Param("inicio") LocalDateTime inicio,
                                  @Param("fim") LocalDateTime fim);

    /** Devolve null quando não há pedidos no período. */
    @Query("""
            select sum(p.valorTotal) from Pedido p
            where p.status = :status and p.pagoEm >= :inicio and p.pagoEm < :fim
            """)
    BigDecimal somarValorPorStatusNoPeriodo(@Param("status") StatusPedido status,
                                            @Param("inicio") LocalDateTime inicio,
                                            @Param("fim") LocalDateTime fim);
}
