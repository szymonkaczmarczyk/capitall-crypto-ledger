package com.capitall.service;

import com.capitall.dto.DashboardStatsResponse;
import com.capitall.dto.PnLPoint;
import com.capitall.exception.ResourceNotFoundException;
import com.capitall.model.User;
import com.capitall.model.UserRole;
import com.capitall.repository.AllocationRepository;
import com.capitall.repository.ExchangeAccountRepository;
import com.capitall.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceImplTest {

    @Mock
    private ExchangeAccountRepository exchangeAccountRepository;

    @Mock
    private AllocationRepository allocationRepository;

    @Mock
    private UserRepository userRepository;

    private AnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        analyticsService = new AnalyticsServiceImpl(exchangeAccountRepository, allocationRepository, userRepository);
    }

    @Test
    void getDashboardStats_ShouldReturnAumAndDiversificationMap() {
        UUID userId = UUID.randomUUID();
        com.capitall.model.ExchangeAccount binance = new com.capitall.model.ExchangeAccount();
        binance.setExchangeName("BINANCE");
        binance.setAllocatedCapital(BigDecimal.valueOf(100000));

        com.capitall.model.ExchangeAccount kraken = new com.capitall.model.ExchangeAccount();
        kraken.setExchangeName("KRAKEN");
        kraken.setAllocatedCapital(BigDecimal.valueOf(50000));

        com.capitall.model.Allocation alloc1 = new com.capitall.model.Allocation();
        alloc1.setExchangeAccount(binance);
        alloc1.setStatus(com.capitall.model.AllocationStatus.ACTIVE);

        com.capitall.model.Allocation alloc2 = new com.capitall.model.Allocation();
        alloc2.setExchangeAccount(kraken);
        alloc2.setStatus(com.capitall.model.AllocationStatus.ACTIVE);

        when(allocationRepository.findByUserId(userId)).thenReturn(List.of(alloc1, alloc2));

        DashboardStatsResponse result = analyticsService.getDashboardStats(userId);

        assertThat(result).isNotNull();
        assertThat(result.totalAum()).isEqualByComparingTo(BigDecimal.valueOf(150000));
        assertThat(result.diversification()).hasSize(2);
        assertThat(result.diversification().get("BINANCE")).isEqualTo(66.67);
        assertThat(result.diversification().get("KRAKEN")).isEqualTo(33.33);
    }

    @Test
    void simulatePnL_ShouldThrowException_WhenUserNotFound() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> analyticsService.simulatePnL(userId, 10))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void simulatePnL_ShouldReturnDeterministicCurve() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).username("bob").role(UserRole.USER).build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(allocationRepository.findByUserId(userId)).thenReturn(Collections.emptyList());

        List<PnLPoint> run1 = analyticsService.simulatePnL(userId, 10);
        List<PnLPoint> run2 = analyticsService.simulatePnL(userId, 10);

        assertThat(run1).hasSize(11);
        assertThat(run2).hasSize(11);

        PnLPoint start = run1.get(0);
        assertThat(start.capital()).isEqualByComparingTo(BigDecimal.valueOf(10000));
        assertThat(start.pnlPercentage()).isEqualByComparingTo(BigDecimal.ZERO);

        for (int i = 0; i < run1.size(); i++) {
            assertThat(run1.get(i).capital()).isEqualTo(run2.get(i).capital());
            assertThat(run1.get(i).pnlPercentage()).isEqualTo(run2.get(i).pnlPercentage());
        }
    }
}
