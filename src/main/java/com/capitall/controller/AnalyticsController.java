package com.capitall.controller;

import com.capitall.dto.DashboardStatsResponse;
import com.capitall.dto.PnLPoint;
import com.capitall.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardStatsResponse> getDashboardStats() {
        DashboardStatsResponse stats = analyticsService.getDashboardStats();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/pnl/{userId}")
    public ResponseEntity<List<PnLPoint>> getPnLSimulation(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "30") int days) {

        List<PnLPoint> simulation = analyticsService.simulatePnL(userId, days);
        return ResponseEntity.ok(simulation);
    }
}
