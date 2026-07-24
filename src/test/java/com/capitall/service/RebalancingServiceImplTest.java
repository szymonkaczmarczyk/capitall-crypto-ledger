package com.capitall.service;

import com.capitall.dto.RebalanceResultDto;
import com.capitall.model.Holding;
import com.capitall.model.TargetAllocation;
import com.capitall.model.Wallet;
import com.capitall.repository.HoldingRepository;
import com.capitall.repository.TargetAllocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class RebalancingServiceImplTest {

    private TargetAllocationRepository targetAllocationRepository;
    private HoldingRepository holdingRepository;
    private WalletService walletService;
    private SecuritiesPriceService priceService;
    private RebalancingServiceImpl rebalancingService;

    private UUID userId;

    @BeforeEach
    public void setUp() {
        targetAllocationRepository = mock(TargetAllocationRepository.class);
        holdingRepository = mock(HoldingRepository.class);
        walletService = mock(WalletService.class);
        priceService = mock(SecuritiesPriceService.class);

        rebalancingService = new RebalancingServiceImpl(
                targetAllocationRepository,
                holdingRepository,
                walletService,
                priceService
        );

        userId = UUID.randomUUID();
    }

    @Test
    public void testSaveTargets_ValidationSuccess() {
        List<TargetAllocation> targets = List.of(
                new TargetAllocation(userId, "BTC", new BigDecimal("0.50")),
                new TargetAllocation(userId, "ETH", new BigDecimal("0.30")),
                new TargetAllocation(userId, "USD", new BigDecimal("0.20"))
        );

        rebalancingService.saveTargets(userId, targets);

        verify(targetAllocationRepository).deleteByUserId(userId);
        verify(targetAllocationRepository, times(3)).save(any(TargetAllocation.class));
    }

    @Test
    public void testSaveTargets_ValidationFailure() {
        List<TargetAllocation> targets = List.of(
                new TargetAllocation(userId, "BTC", new BigDecimal("0.50")),
                new TargetAllocation(userId, "ETH", new BigDecimal("0.40"))
        );

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            rebalancingService.saveTargets(userId, targets);
        });

        assertTrue(exception.getMessage().contains("Suma alokacji docelowych musi wynosić 100%"));
    }

    @Test
    public void testCalculateRebalancing() {
        // Setup Wallet
        Wallet wallet = new Wallet(userId, new BigDecimal("200.00"));
        when(walletService.getOrCreate(userId)).thenReturn(wallet);

        // Setup Holdings
        Holding btcHolding = new Holding(userId, "BTC");
        btcHolding.setAmount(new BigDecimal("1.00"));
        btcHolding.setAvgCost(new BigDecimal("50.00"));

        Holding ethHolding = new Holding(userId, "ETH");
        ethHolding.setAmount(new BigDecimal("2.00"));
        ethHolding.setAvgCost(new BigDecimal("25.00"));

        List<Holding> holdings = List.of(btcHolding, ethHolding);
        when(holdingRepository.findByUserId(userId)).thenReturn(holdings);

        // Setup live prices
        when(priceService.getCurrentPrice("BTC")).thenReturn(new BigDecimal("100.00"));
        when(priceService.getCurrentPrice("ETH")).thenReturn(new BigDecimal("50.00"));

        // Setup Target Allocations: 50% BTC ($200), 25% ETH ($100), 25% USD ($100)
        List<TargetAllocation> targets = List.of(
                new TargetAllocation(userId, "BTC", new BigDecimal("0.50")),
                new TargetAllocation(userId, "ETH", new BigDecimal("0.25")),
                new TargetAllocation(userId, "USD", new BigDecimal("0.25"))
        );
        when(targetAllocationRepository.findByUserId(userId)).thenReturn(targets);

        RebalanceResultDto result = rebalancingService.calculateRebalancing(userId);

        // Total Portfolio = 1 BTC * $100 + 2 ETH * $50 + $200 USD = $400 USD
        assertEquals(new BigDecimal("400.00"), result.getTotalPortfolioValue());

        // Check Allocations
        // BTC: current value = 100 (25%), target value = 200 (50%), diff = +100
        RebalanceResultDto.TargetDetail btcTarget = result.getTargetAllocations().stream()
                .filter(t -> t.getSymbol().equals("BTC"))
                .findFirst().orElseThrow();
        assertEquals(new BigDecimal("200.00"), btcTarget.getTargetValue());
        assertEquals(new BigDecimal("100.00"), btcTarget.getDiffValue());

        // Recommendations: BTC needs +$100 value -> BUY 1.0000 BTC at $100/BTC
        RebalanceResultDto.Recommendation btcRec = result.getRecommendations().stream()
                .filter(r -> r.getSymbol().equals("BTC"))
                .findFirst().orElseThrow();
        assertEquals("BUY", btcRec.getAction());
        assertEquals(new BigDecimal("1.000000"), btcRec.getQuantity());
        assertEquals(new BigDecimal("100.00"), btcRec.getValueInUsd());
    }
}
