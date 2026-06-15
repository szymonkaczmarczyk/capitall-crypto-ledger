package com.capitall.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ExchangeAccountResponse(
    UUID id,
    String exchangeName,
    String accountName,
    BigDecimal allocatedCapital,
    boolean isActive,
    LocalDateTime createdAt
) {}
