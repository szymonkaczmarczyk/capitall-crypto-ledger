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

import org.springframework.security.test.context.support.WithMockUser;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@WebMvcTest(controllers = AnalyticsController.class)
@WithMockUser(username = "bob", roles = "USER")
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnalyticsService analyticsService;

    @MockBean
    private com.capitall.config.MaintenanceModeState maintenanceModeState;

    @MockBean
    private com.capitall.repository.UserRepository userRepository;

    @MockBean
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Test
    void getDashboardStats_ShouldReturnStats() throws Exception {
        UUID userId = UUID.randomUUID();
        com.capitall.model.User mockUser = com.capitall.model.User.builder()
                .id(userId)
                .username("bob")
                .email("bob@example.com")
                .role(com.capitall.model.UserRole.USER)
                .build();

        DashboardStatsResponse stats = new DashboardStatsResponse(
                BigDecimal.valueOf(250000), Map.of("BINANCE", 70.0, "KRAKEN", 30.0)
        );
        when(analyticsService.getDashboardStats(org.mockito.ArgumentMatchers.any(UUID.class))).thenReturn(stats);

        mockMvc.perform(get("/api/analytics/dashboard")
                .with(user(mockUser))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAum").value(250000))
                .andExpect(jsonPath("$.diversification.BINANCE").value(70.0))
                .andExpect(jsonPath("$.diversification.KRAKEN").value(30.0));
    }

    @Test
    void getPnLSimulation_ShouldReturnTimeline() throws Exception {
        UUID userId = UUID.randomUUID();
        com.capitall.model.User mockUser = com.capitall.model.User.builder()
                .id(userId)
                .username("bob")
                .email("bob@example.com")
                .role(com.capitall.model.UserRole.USER)
                .build();

        PnLPoint point = new PnLPoint(LocalDateTime.now(), BigDecimal.valueOf(10200), BigDecimal.valueOf(2.0));

        when(analyticsService.simulatePnL(userId, 30)).thenReturn(List.of(point));

        mockMvc.perform(get("/api/analytics/pnl/" + userId)
                .with(user(mockUser))
                .param("days", "30")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].capital").value(10200))
                .andExpect(jsonPath("$[0].pnlPercentage").value(2.0));
    }
}
