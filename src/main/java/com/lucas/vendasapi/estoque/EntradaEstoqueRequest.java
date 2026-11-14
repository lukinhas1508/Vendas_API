package com.lucas.vendasapi.estoque;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record EntradaEstoqueRequest(
        @NotNull @Positive Integer quantidade,
        @NotBlank @Size(max = 200) String motivo) {
}
