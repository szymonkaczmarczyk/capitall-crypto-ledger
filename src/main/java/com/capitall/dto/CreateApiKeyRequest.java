package com.capitall.dto;

import com.capitall.model.Exchange;
import com.capitall.model.Permission;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.Set;

public record CreateApiKeyRequest(
    @NotBlank(message = "Title must not be blank")
    @Size(min = 2, max = 100, message = "Title must be between 2 and 100 characters")
    String title,

    @NotBlank(message = "Public key must not be blank")
    @Size(max = 255, message = "Public key must not exceed 255 characters")
    String publicKey,

    @NotBlank(message = "Secret key must not be blank")
    @Size(max = 255, message = "Secret key must not exceed 255 characters")
    String secretKey,

    @NotNull(message = "Exchange must not be null")
    Exchange exchange,

    @NotEmpty(message = "Permissions must not be empty")
    Set<Permission> permissions,

    @NotNull(message = "Pool ID must not be null")
    Long poolId,

    @NotNull(message = "Trader ID must not be null")
    Long traderId,

    @Future(message = "Expiration time must be in the future")
    LocalDateTime expiresAt
) {}
