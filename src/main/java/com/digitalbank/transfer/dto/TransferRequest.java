package com.digitalbank.transfer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Dados para realização de uma transferência entre contas")
public record TransferRequest(
    @Schema(description = "UUID gerado pelo cliente para garantir idempotência", example = "550e8400-e29b-41d4-a716-446655440000")
    @NotNull(message = "transferId é obrigatório")
    UUID transferId,

    @Schema(description = "ID da conta de origem", example = "550e8400-e29b-41d4-a716-446655440001")
    @NotNull(message = "sourceAccountId é obrigatório")
    UUID sourceAccountId,

    @Schema(description = "ID da conta de destino", example = "550e8400-e29b-41d4-a716-446655440002")
    @NotNull(message = "targetAccountId é obrigatório")
    UUID targetAccountId,

    @Schema(description = "Valor a ser transferido", example = "200.00")
    @NotNull(message = "amount é obrigatório")
    @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
    BigDecimal amount
) { }
