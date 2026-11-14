package com.lucas.vendasapi.cliente;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lucas.vendasapi.exception.ConflitoException;
import com.lucas.vendasapi.exception.RecursoNaoEncontradoException;
import com.lucas.vendasapi.pedido.PedidoRepository;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private PedidoRepository pedidoRepository;

    @InjectMocks
    private ClienteService clienteService;

    private final ClienteRequest request =
            new ClienteRequest("Ana Souza", " Ana@Email.com ", "529.982.247-25", "11999990000");

    @Test
    @DisplayName("criar: salva o cliente com e-mail em minúsculas e CPF só com dígitos")
    void criarCliente() {
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(chamada -> chamada.getArgument(0));

        ClienteResponse cliente = clienteService.criar(request);

        assertThat(cliente.nome()).isEqualTo("Ana Souza");
        assertThat(cliente.email()).isEqualTo("ana@email.com");
        assertThat(cliente.cpf()).isEqualTo("52998224725");
    }

    @Test
    @DisplayName("criar: e-mail já cadastrado gera ConflitoException")
    void criarClienteEmailDuplicado() {
        when(clienteRepository.existsByEmailIgnoreCase("ana@email.com")).thenReturn(true);

        assertThatThrownBy(() -> clienteService.criar(request))
                .isInstanceOf(ConflitoException.class)
                .hasMessageContaining("e-mail");

        verify(clienteRepository, never()).save(any(Cliente.class));
    }

    @Test
    @DisplayName("criar: CPF já cadastrado gera ConflitoException")
    void criarClienteCpfDuplicado() {
        when(clienteRepository.existsByCpf("52998224725")).thenReturn(true);

        assertThatThrownBy(() -> clienteService.criar(request))
                .isInstanceOf(ConflitoException.class)
                .hasMessageContaining("CPF");

        verify(clienteRepository, never()).save(any(Cliente.class));
    }

    @Test
    @DisplayName("buscarPorId: id inexistente gera RecursoNaoEncontradoException")
    void buscarClienteInexistente() {
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteService.buscarPorId(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("atualizar: altera os dados do cliente existente")
    void atualizarCliente() {
        Cliente existente = new Cliente("Ana", "antigo@email.com", "52998224725", null);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(existente));

        ClienteResponse cliente = clienteService.atualizar(1L, request);

        assertThat(cliente.nome()).isEqualTo("Ana Souza");
        assertThat(existente.getEmail()).isEqualTo("ana@email.com");
    }

    @Test
    @DisplayName("excluir: cliente com pedidos não pode ser excluído")
    void excluirClienteComPedidos() {
        Cliente existente = new Cliente("Ana", "ana@email.com", "52998224725", null);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(pedidoRepository.existsByClienteId(1L)).thenReturn(true);

        assertThatThrownBy(() -> clienteService.excluir(1L))
                .isInstanceOf(ConflitoException.class);

        verify(clienteRepository, never()).delete(any(Cliente.class));
    }

    @Test
    @DisplayName("excluir: cliente sem pedidos é removido")
    void excluirClienteSemPedidos() {
        Cliente existente = new Cliente("Ana", "ana@email.com", "52998224725", null);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(existente));

        clienteService.excluir(1L);

        verify(clienteRepository).delete(existente);
    }
}
