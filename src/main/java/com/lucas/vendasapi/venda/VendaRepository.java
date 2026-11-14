package com.lucas.vendasapi.venda;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.lucas.vendasapi.pedido.ItemPedido;
import com.lucas.vendasapi.pedido.StatusPedido;

/** Consultas somente de leitura para os relatórios de vendas. */
public interface VendaRepository extends Repository<ItemPedido, Long> {

    @Query("""
            select pr.id as produtoId,
                   pr.nome as nome,
                   sum(i.quantidade) as quantidadeVendida,
                   sum(i.precoUnitario * i.quantidade) as totalVendido
            from ItemPedido i
            join i.pedido p
            join i.produto pr
            where p.status = :status and p.pagoEm >= :inicio and p.pagoEm < :fim
            group by pr.id, pr.nome
            order by sum(i.quantidade) desc
            """)
    List<ProdutoMaisVendido> buscarProdutosMaisVendidos(@Param("status") StatusPedido status,
                                                        @Param("inicio") LocalDateTime inicio,
                                                        @Param("fim") LocalDateTime fim,
                                                        Pageable pageable);
}
