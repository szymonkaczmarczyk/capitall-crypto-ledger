package com.capitall.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreatePoolRequest(
    @NotBlank(message = "Pool name must not be blank")
    @Size(min = 2, max = 100, message = "Pool name must be between 2 and 100 characters")
    String name,

    @Size(max = 500, message = "Description must not exceed 500 characters")
    String description,

    @NotNull(message = "Initial balance must not be null")
    @DecimalMin(value = "0.0", message = "Initial balance must be zero or positive")
    BigDecimal initialBalance,

    @NotBlank(message = "Currency must not be blank")
    @Size(min = 3, max = 10, message = "Currency code must be between 3 and 10 characters")
    String currency,

    @NotNull(message = "Manager ID must not be null")
    Long managerId
) {}
