package com.capitall.dto;

import com.capitall.model.PoolStatus;
import jakarta.validation.constraints.NotNull;

public record UpdatePoolStatusRequest(
    @NotNull(message = "Pool status must not be null")
    PoolStatus status
) {}
