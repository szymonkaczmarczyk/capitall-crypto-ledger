package com.capitall.dto;

import java.math.BigDecimal;
import java.util.Map;

public record DashboardStatsResponse(
    BigDecimal totalAum,
    Map<String, Double> diversification
) {}
