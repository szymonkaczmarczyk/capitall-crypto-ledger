package com.capitall.dto;

import com.capitall.model.Exchange;
import com.capitall.model.KeyStatus;
import com.capitall.model.Permission;
import java.time.LocalDateTime;
import java.util.Set;

public record ApiKeyCreatedResponse(
    Long id,
    String title,
    String publicKey,
    String secretKey,
    Exchange exchange,
    Set<Permission> permissions,
    KeyStatus status,
    Long poolId,
    Long traderId,
    LocalDateTime createdAt,
    LocalDateTime expiresAt
) {}
