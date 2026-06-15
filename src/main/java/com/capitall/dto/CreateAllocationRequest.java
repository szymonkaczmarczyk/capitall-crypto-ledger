package com.capitall.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;

public record CreateAllocationRequest(
    @NotNull(message = "User ID must not be null")
    UUID userId,

    @NotNull(message = "Exchange Account ID must not be null")
    UUID exchangeAccountId,

    @Future(message = "Expiration time must be in the future")
    LocalDateTime expiresAt
) {}
