package com.capitall.dto;

import com.capitall.model.Exchange;
import com.capitall.model.KeyStatus;
import com.capitall.model.Permission;
import java.time.LocalDateTime;
import java.util.Set;

public record ApiKeyDto(
    Long id,
    String title,
    String publicKey,
    Exchange exchange,
    Set<Permission> permissions,
    KeyStatus status,
    Long poolId,
    String poolName,
    Long traderId,
    String traderName,
    LocalDateTime createdAt,
    LocalDateTime expiresAt
) {}
