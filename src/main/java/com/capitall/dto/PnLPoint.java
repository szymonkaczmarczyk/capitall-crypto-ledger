package com.capitall.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PnLPoint(
    LocalDateTime timestamp,
    BigDecimal capital,
    BigDecimal pnlPercentage
) {}
