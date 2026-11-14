package com.lucas.vendasapi.cliente;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.lucas.vendasapi.exception.GlobalExceptionHandler;
import com.lucas.vendasapi.exception.RecursoNaoEncontradoException;

/**
 * Testa a camada web isolada (rotas, validação, códigos HTTP e tratamento de erros)
 * com MockMvc "standalone": sem subir o contexto do Spring nem o banco.
 */
@ExtendWith(MockitoExtension.class)
class ClienteControllerTest {

    @Mock
    private ClienteService clienteService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ClienteController(clienteService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/clientes: dados válidos devolvem 201 com Location")
    void criarClienteValido() throws Exception {
        when(clienteService.criar(any(ClienteRequest.class))).thenReturn(new ClienteResponse(
                1L, "Ana Souza", "ana@email.com", "52998224725", "11999990000", LocalDateTime.now()));

        mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Ana Souza",
                                  "email": "ana@email.com",
                                  "cpf": "529.982.247-25",
                                  "telefone": "11999990000"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/clientes/1")))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nome").value("Ana Souza"));
    }

    @Test
    @DisplayName("POST /api/clientes: dados inválidos devolvem 400 e não chamam o service")
    void criarClienteInvalido() throws Exception {
        mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "",
                                  "email": "nao-e-um-email",
                                  "cpf": "11111111111"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(clienteService);
    }

    @Test
    @DisplayName("GET /api/clientes/{id}: cliente inexistente devolve 404")
    void buscarClienteInexistente() throws Exception {
        when(clienteService.buscarPorId(99L)).thenThrow(new RecursoNaoEncontradoException("Cliente", 99L));

        mockMvc.perform(get("/api/clientes/99"))
                .andExpect(status().isNotFound());
    }
}
