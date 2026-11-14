package com.lucas.vendasapi.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

/** Metadados da documentação e o botão "Authorize" (Bearer JWT) do Swagger UI. */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Vendas API",
                version = "1.0.0",
                description = "API REST para gestão de clientes, produtos, pedidos, estoque e vendas."),
        security = @SecurityRequirement(name = OpenApiConfig.ESQUEMA_BEARER))
@SecurityScheme(
        name = OpenApiConfig.ESQUEMA_BEARER,
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT")
public class OpenApiConfig {

    public static final String ESQUEMA_BEARER = "bearerAuth";
}
