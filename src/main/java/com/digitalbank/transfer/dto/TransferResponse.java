package com.digitalbank.transfer.dto;

import com.digitalbank.transfer.domain.Transfer;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransferResponse(
        UUID transferId,
        UUID sourceAccountId,
        UUID targetAccountId,
        BigDecimal amount,
        LocalDateTime createdAt
) {
    public static TransferResponse from(Transfer transfer) {
        return new TransferResponse(
            transfer.getId(),
            transfer.getSourceAccountId(),
            transfer.getTargetAccountId(),
            transfer.getAmount(),
            transfer.getCreatedAt());
    }
}
