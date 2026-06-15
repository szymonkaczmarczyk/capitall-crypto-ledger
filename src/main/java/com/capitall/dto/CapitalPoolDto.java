package com.capitall.dto;

import com.capitall.model.PoolStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CapitalPoolDto(
    Long id,
    String name,
    String description,
    BigDecimal balance,
    String currency,
    PoolStatus status,
    Long managerId,
    String managerName,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
