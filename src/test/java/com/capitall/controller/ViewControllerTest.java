package com.capitall.controller;

import com.capitall.dto.DashboardStatsResponse;
import com.capitall.dto.ExchangeAccountResponse;
import com.capitall.dto.PnLPoint;
import com.capitall.dto.UserDto;
import com.capitall.model.UserRole;
import com.capitall.service.AnalyticsService;
import com.capitall.service.ExchangeAccountService;
import com.capitall.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.security.test.context.support.WithMockUser;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@WebMvcTest(ViewController.class)
@WithMockUser(username = "admin", roles = { "USER", "ADMIN" })
class ViewControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private ExchangeAccountService exchangeAccountService;

        @MockBean
        private AnalyticsService analyticsService;

        @MockBean
        private UserService userService;

        @MockBean
        private com.capitall.service.AllocationService allocationService;

        @MockBean
        private com.capitall.repository.UserRepository userRepository;

        @MockBean
        private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

        @MockBean
        private com.capitall.service.AuditLogService auditLogService;

        @MockBean
        private com.capitall.config.MaintenanceModeState maintenanceModeState;

        @MockBean
        private com.capitall.repository.AllocationRepository allocationRepository;

        @MockBean
        private com.capitall.repository.WalletRepository walletRepository;

        @MockBean
        private com.capitall.repository.HoldingRepository holdingRepository;

        @MockBean
        private com.capitall.repository.TradeRepository tradeRepository;

        @MockBean
        private com.capitall.service.WalletService walletService;

        @MockBean
        private com.capitall.service.EquitySnapshotter equitySnapshotter;

        @MockBean
        private com.capitall.repository.EquitySnapshotRepository equitySnapshotRepository;

        @Test
        void dashboard_ShouldPopulateStatsAndRenderView() throws Exception {
                DashboardStatsResponse stats = new DashboardStatsResponse(
                                BigDecimal.valueOf(150000), Map.of("BINANCE", 100.0));
                UUID userId = UUID.randomUUID();
                UserDto user = new UserDto(userId, "bob", "bob@example.com", UserRole.USER, true);

                when(analyticsService.getDashboardStats()).thenReturn(stats);
                when(userService.getAllUsers()).thenReturn(List.of(user));
                when(analyticsService.simulatePnL(eq(userId), eq(30)))
                                .thenReturn(List.of(new PnLPoint(LocalDateTime.now(), BigDecimal.valueOf(10000),
                                                BigDecimal.ZERO)));

                com.capitall.model.User mockUser = com.capitall.model.User.builder()
                                .id(userId)
                                .username("bob")
                                .email("bob@example.com")
                                .role(com.capitall.model.UserRole.USER)
                                .build();

                mockMvc.perform(get("/dashboard").with(user(mockUser)))
                                .andExpect(status().isOk())
                                .andExpect(view().name("dashboard"))
                                .andExpect(model().attributeExists("stats"))
                                .andExpect(model().attributeExists("pnlPoints"))
                                .andExpect(model().attribute("traderName", "bob"));
        }

        @Test
        void listAssets_ShouldReturnFilteredAssetsAndView() throws Exception {
                UUID assetId = UUID.randomUUID();
                ExchangeAccountResponse response = new ExchangeAccountResponse(
                                assetId, "BINANCE", "Binance Bot", BigDecimal.valueOf(5000), true, LocalDateTime.now());

                when(exchangeAccountService.searchExchangeAccounts(eq("BINANCE"), eq(BigDecimal.valueOf(1000)),
                                eq(true), any(Sort.class)))
                                .thenReturn(List.of(response));

                com.capitall.model.User mockUser = com.capitall.model.User.builder()
                                .id(UUID.randomUUID())
                                .username("admin")
                                .role(com.capitall.model.UserRole.ADMIN)
                                .build();

                mockMvc.perform(get("/assets")
                                .with(user(mockUser))
                                .param("exchange", "BINANCE")
                                .param("minCapital", "1000")
                                .param("active", "true"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("assets"))
                                .andExpect(model().attributeExists("assets"))
                                .andExpect(model().attribute("exchange", "BINANCE"))
                                .andExpect(model().attribute("minCapital", BigDecimal.valueOf(1000)))
                                .andExpect(model().attribute("active", true));
        }

        @Test
        void showCreateForm_ShouldRenderForm() throws Exception {
                mockMvc.perform(get("/assets/new"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("asset-form"))
                                .andExpect(model().attributeExists("exchangeAccountForm"))
                                .andExpect(model().attribute("isEdit", false));
        }

        @Test
        void submitCreateForm_ShouldRedirect_WhenValidationPasses() throws Exception {
                mockMvc.perform(post("/assets/new")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .param("exchangeName", "BINANCE")
                                .param("accountName", "My Bot")
                                .param("apiKey", "api-key-test")
                                .param("apiSecret", "api-secret-test")
                                .param("allocatedCapital", "2000"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/assets"));
        }

        @Test
        void submitCreateForm_ShouldRenderFormWithErrors_WhenValidationFails() throws Exception {
                mockMvc.perform(post("/assets/new")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .param("exchangeName", "")
                                .param("accountName", "")
                                .param("apiKey", "api-key-test")
                                .param("apiSecret", "api-secret-test")
                                .param("allocatedCapital", "-50"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("asset-form"))
                                .andExpect(model().attributeHasFieldErrors("exchangeAccountForm", "exchangeName",
                                                "accountName", "allocatedCapital"));
        }
}
