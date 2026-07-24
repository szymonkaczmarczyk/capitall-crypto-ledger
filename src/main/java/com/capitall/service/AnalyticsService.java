package com.capitall.service;

import com.capitall.dto.DashboardStatsResponse;
import com.capitall.dto.PnLPoint;
import com.capitall.dto.PortfolioAnalyticsResponse;

import java.util.List;
import java.util.UUID;

public interface AnalyticsService {
    DashboardStatsResponse getDashboardStats(UUID userId);
    List<PnLPoint> simulatePnL(UUID userId, int days);
    PortfolioAnalyticsResponse getPortfolioAnalytics(UUID userId);
}

