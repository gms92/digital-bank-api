package com.digitalbank.transfer.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "statements")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Statement {

    @Id
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "transfer_id", nullable = false)
    private UUID transferId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private StatementType type;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Statement(UUID accountId, UUID transferId, StatementType type, BigDecimal amount) {
        this.id = UUID.randomUUID();
        this.accountId = accountId;
        this.transferId = transferId;
        this.type = type;
        this.amount = amount;
        this.createdAt = LocalDateTime.now();
    }
}
