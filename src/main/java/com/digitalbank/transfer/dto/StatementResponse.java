package com.digitalbank.transfer.dto;

import com.digitalbank.transfer.domain.Statement;
import com.digitalbank.transfer.domain.StatementType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record StatementResponse(
        UUID id, UUID transferId, StatementType type, BigDecimal amount, LocalDateTime createdAt) {
    public static StatementResponse from(Statement statement) {
        return new StatementResponse(
            statement.getId(),
            statement.getTransferId(),
            statement.getType(),
            statement.getAmount(),
            statement.getCreatedAt());
    }
}
