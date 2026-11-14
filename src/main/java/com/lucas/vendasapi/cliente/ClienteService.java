package com.lucas.vendasapi.cliente;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lucas.vendasapi.exception.ConflitoException;
import com.lucas.vendasapi.exception.RecursoNaoEncontradoException;
import com.lucas.vendasapi.pedido.PedidoRepository;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final PedidoRepository pedidoRepository;

    public ClienteService(ClienteRepository clienteRepository, PedidoRepository pedidoRepository) {
        this.clienteRepository = clienteRepository;
        this.pedidoRepository = pedidoRepository;
    }

    @Transactional
    public ClienteResponse criar(ClienteRequest request) {
        String email = normalizarEmail(request.email());
        String cpf = somenteDigitos(request.cpf());

        if (clienteRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflitoException("Já existe um cliente com o e-mail informado.");
        }
        if (clienteRepository.existsByCpf(cpf)) {
            throw new ConflitoException("Já existe um cliente com o CPF informado.");
        }

        Cliente cliente = new Cliente(request.nome().trim(), email, cpf, request.telefone());
        return ClienteResponse.from(clienteRepository.save(cliente));
    }

    @Transactional(readOnly = true)
    public Page<ClienteResponse> listar(Pageable pageable) {
        return clienteRepository.findAll(pageable).map(ClienteResponse::from);
    }

    @Transactional(readOnly = true)
    public ClienteResponse buscarPorId(Long id) {
        return ClienteResponse.from(buscarEntidade(id));
    }

    @Transactional
    public ClienteResponse atualizar(Long id, ClienteRequest request) {
        Cliente cliente = buscarEntidade(id);
        String email = normalizarEmail(request.email());
        String cpf = somenteDigitos(request.cpf());

        if (clienteRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new ConflitoException("Já existe outro cliente com o e-mail informado.");
        }
        if (clienteRepository.existsByCpfAndIdNot(cpf, id)) {
            throw new ConflitoException("Já existe outro cliente com o CPF informado.");
        }

        // entidade gerenciada: o Hibernate grava as mudanças ao fim da transação
        cliente.atualizar(request.nome().trim(), email, cpf, request.telefone());
        return ClienteResponse.from(cliente);
    }

    @Transactional
    public void excluir(Long id) {
        Cliente cliente = buscarEntidade(id);
        if (pedidoRepository.existsByClienteId(id)) {
            throw new ConflitoException("Cliente possui pedidos e não pode ser excluído.");
        }
        clienteRepository.delete(cliente);
    }

    private Cliente buscarEntidade(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente", id));
    }

    private static String normalizarEmail(String email) {
        return email.trim().toLowerCase();
    }

    private static String somenteDigitos(String cpf) {
        return cpf.replaceAll("\\D", "");
    }
}
