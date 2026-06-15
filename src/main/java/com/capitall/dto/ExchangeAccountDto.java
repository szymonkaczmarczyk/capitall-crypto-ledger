package com.capitall.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ExchangeAccountDto(
    UUID id,
    String exchangeName,
    String accountName,
    String apiKey,
    String apiSecret,
    BigDecimal allocatedCapital,
    boolean isActive,
    LocalDateTime createdAt
) {}
