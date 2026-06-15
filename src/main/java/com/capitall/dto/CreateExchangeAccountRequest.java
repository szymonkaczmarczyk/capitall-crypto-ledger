package com.capitall.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateExchangeAccountRequest(
    @NotBlank(message = "Nazwa giełdy nie może być pusta")
    @Size(max = 50, message = "Nazwa giełdy nie może przekraczać 50 znaków")
    String exchangeName,

    @NotBlank(message = "Nazwa konta nie może być pusta")
    @Size(min = 2, max = 100, message = "Nazwa konta musi mieć od 2 do 100 znaków")
    String accountName,

    @NotBlank(message = "Klucz API nie może być pusty")
    @Size(max = 255, message = "Klucz API nie może przekraczać 255 znaków")
    String apiKey,

    @NotBlank(message = "Sekret API nie może być pusty")
    @Size(max = 255, message = "Sekret API nie może przekraczać 255 znaków")
    String apiSecret,

    @NotNull(message = "Kapitał nie może być pusty")
    @DecimalMin(value = "0.01", message = "Kapitał musi być większy od zera")
    BigDecimal allocatedCapital
) {}
