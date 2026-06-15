package com.capitall.dto;

import com.capitall.model.AllocationStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record AllocationDto(
    UUID id,
    UUID userId,
    String username,
    String userRole,
    UUID exchangeAccountId,
    String exchangeName,
    String accountName,
    java.math.BigDecimal allocatedCapital,
    LocalDateTime assignedAt,
    LocalDateTime expiresAt,
    AllocationStatus status
) {}
