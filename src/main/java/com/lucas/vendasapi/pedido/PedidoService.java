package com.lucas.vendasapi.pedido;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lucas.vendasapi.cliente.Cliente;
import com.lucas.vendasapi.cliente.ClienteRepository;
import com.lucas.vendasapi.estoque.EstoqueService;
import com.lucas.vendasapi.exception.RecursoNaoEncontradoException;
import com.lucas.vendasapi.exception.RegraNegocioException;
import com.lucas.vendasapi.produto.Produto;
import com.lucas.vendasapi.produto.ProdutoRepository;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ClienteRepository clienteRepository;
    private final ProdutoRepository produtoRepository;
    private final EstoqueService estoqueService;

    public PedidoService(PedidoRepository pedidoRepository, ClienteRepository clienteRepository,
                         ProdutoRepository produtoRepository, EstoqueService estoqueService) {
        this.pedidoRepository = pedidoRepository;
        this.clienteRepository = clienteRepository;
        this.produtoRepository = produtoRepository;
        this.estoqueService = estoqueService;
    }

    /**
     * Cria o pedido e já reserva (baixa) o estoque de cada item.
     * Tudo roda em uma transação: se faltar estoque para um item, nada é gravado.
     */
    @Transactional
    public PedidoResponse criar(PedidoRequest request) {
        Cliente cliente = clienteRepository.findById(request.clienteId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente", request.clienteId()));

        Pedido pedido = new Pedido(cliente);
        for (ItemPedidoRequest item : request.itens()) {
            Produto produto = produtoRepository.findById(item.produtoId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Produto", item.produtoId()));
            if (!produto.isAtivo()) {
                throw new RegraNegocioException(
                        "O produto '%s' está inativo e não pode ser vendido.".formatted(produto.getNome()));
            }
            estoqueService.registrarSaida(produto, item.quantidade(), "Venda");
            pedido.adicionarItem(produto, item.quantidade());
        }

        return PedidoResponse.from(pedidoRepository.save(pedido));
    }

    @Transactional(readOnly = true)
    public Page<PedidoResponse> listar(StatusPedido status, Pageable pageable) {
        Page<Pedido> pedidos = (status == null)
                ? pedidoRepository.findAll(pageable)
                : pedidoRepository.findByStatus(status, pageable);
        return pedidos.map(PedidoResponse::from);
    }

    @Transactional(readOnly = true)
    public PedidoResponse buscarPorId(Long id) {
        return PedidoResponse.from(buscarEntidade(id));
    }

    /** Confirma o pagamento: a partir daqui o pedido conta como venda nos relatórios. */
    @Transactional
    public PedidoResponse pagar(Long id) {
        Pedido pedido = buscarEntidade(id);
        pedido.pagar();
        return PedidoResponse.from(pedido);
    }

    /** Cancela um pedido ainda não pago e devolve os itens ao estoque. */
    @Transactional
    public PedidoResponse cancelar(Long id) {
        Pedido pedido = buscarEntidade(id);
        pedido.cancelar();
        for (ItemPedido item : pedido.getItens()) {
            estoqueService.registrarEntrada(item.getProduto(), item.getQuantidade(),
                    "Cancelamento do pedido " + pedido.getId());
        }
        return PedidoResponse.from(pedido);
    }

    private Pedido buscarEntidade(Long id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido", id));
    }
}
