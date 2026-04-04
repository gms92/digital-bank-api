package com.digitalbank.account.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "Dados para criação de uma conta")
public record AccountRequest(
    @Schema(description = "Nome do titular da conta", example = "Alice")
    @NotBlank(message = "Nome é obrigatório")
    String name,

    @Schema(description = "Saldo inicial da conta", example = "1000.00")
    @NotNull(message = "Saldo inicial é obrigatório")
    @DecimalMin(value = "0.0", message = "Saldo inicial não pode ser negativo")
    BigDecimal initialBalance
) { }
