package com.digitalbank.transfer.domain;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferFundsCompletedEvent(
    UUID transferId,
    UUID sourceAccountId,
    UUID targetAccountId,
    BigDecimal amount
) { }
