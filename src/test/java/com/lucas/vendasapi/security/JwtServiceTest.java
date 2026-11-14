package com.lucas.vendasapi.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SEGREDO = "segredo-apenas-para-testes-com-mais-de-32-caracteres";
    private static final String EMAIL = "ana@email.com";

    private final JwtService jwtService = new JwtService(SEGREDO, 60);

    @Test
    @DisplayName("token gerado devolve o e-mail do usuário")
    void tokenValido() {
        String token = jwtService.gerarToken(EMAIL);

        assertThat(jwtService.extrairEmail(token)).contains(EMAIL);
    }

    @Test
    @DisplayName("token adulterado é rejeitado")
    void tokenAdulterado() {
        String token = jwtService.gerarToken(EMAIL);
        String adulterado = token.substring(0, token.lastIndexOf('.') + 1) + "assinatura-falsa";

        assertThat(jwtService.extrairEmail(adulterado)).isEmpty();
    }

    @Test
    @DisplayName("token assinado com outro segredo é rejeitado")
    void tokenDeOutroSegredo() {
        JwtService outro = new JwtService("outro-segredo-diferente-com-mais-de-32-caracteres", 60);

        assertThat(jwtService.extrairEmail(outro.gerarToken(EMAIL))).isEmpty();
    }

    @Test
    @DisplayName("token expirado é rejeitado")
    void tokenExpirado() {
        JwtService jaExpira = new JwtService(SEGREDO, -5);

        assertThat(jaExpira.extrairEmail(jaExpira.gerarToken(EMAIL))).isEmpty();
    }

    @Test
    @DisplayName("texto que não é um JWT é rejeitado")
    void textoQualquer() {
        assertThat(jwtService.extrairEmail("isso-nao-e-um-token")).isEmpty();
    }

    @Test
    @DisplayName("expiração é informada em segundos")
    void expiracaoEmSegundos() {
        assertThat(jwtService.getExpiracaoEmSegundos()).isEqualTo(3600);
    }
}
