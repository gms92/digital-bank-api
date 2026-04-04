package com.digitalbank.account.dto;

import com.digitalbank.account.domain.Account;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record AccountResponse(UUID id, String name, BigDecimal balance, LocalDateTime createdAt) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(
            account.getId(),
                account.getName(),
                account.getBalance(),
                account.getCreatedAt()
        );
    }
}
