package com.capitall.controller;

import com.capitall.dto.DashboardStatsResponse;
import com.capitall.dto.PnLPoint;
import com.capitall.service.AnalyticsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AnalyticsController.class, excludeAutoConfiguration = {
    org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
    org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
})
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnalyticsService analyticsService;

    @MockBean
    private com.capitall.config.MaintenanceModeState maintenanceModeState;

    @Test
    void getDashboardStats_ShouldReturnStats() throws Exception {
        DashboardStatsResponse stats = new DashboardStatsResponse(
                BigDecimal.valueOf(250000), Map.of("BINANCE", 70.0, "KRAKEN", 30.0)
        );
        when(analyticsService.getDashboardStats()).thenReturn(stats);

        mockMvc.perform(get("/api/analytics/dashboard")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAum").value(250000))
                .andExpect(jsonPath("$.diversification.BINANCE").value(70.0))
                .andExpect(jsonPath("$.diversification.KRAKEN").value(30.0));
    }

    @Test
    void getPnLSimulation_ShouldReturnTimeline() throws Exception {
        UUID userId = UUID.randomUUID();
        PnLPoint point = new PnLPoint(LocalDateTime.now(), BigDecimal.valueOf(10200), BigDecimal.valueOf(2.0));

        when(analyticsService.simulatePnL(userId, 30)).thenReturn(List.of(point));

        mockMvc.perform(get("/api/analytics/pnl/" + userId)
                .param("days", "30")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].capital").value(10200))
                .andExpect(jsonPath("$[0].pnlPercentage").value(2.0));
    }
}
