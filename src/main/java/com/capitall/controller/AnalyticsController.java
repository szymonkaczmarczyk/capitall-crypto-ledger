package com.capitall.controller;

import com.capitall.dto.DashboardStatsResponse;
import com.capitall.dto.PnLPoint;
import com.capitall.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/analytics")
@PreAuthorize("isAuthenticated()")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardStatsResponse> getDashboardStats(
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.capitall.model.User user) {
        UUID uid = (user != null) ? user.getId() : UUID.fromString("00000000-0000-0000-0000-000000000000");
        DashboardStatsResponse stats = analyticsService.getDashboardStats(uid);
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
