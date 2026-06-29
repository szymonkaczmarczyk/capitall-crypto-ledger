package com.capitall.service;

import com.capitall.model.SecurityHolding;
import com.capitall.model.SecurityTrade;
import com.capitall.repository.SecurityHoldingRepository;
import com.capitall.repository.SecurityTradeRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityPortfolioServiceTest {

    @Mock
    private SecurityHoldingRepository holdingRepository;

    @Mock
    private SecurityTradeRepository tradeRepository;

    @Mock
    private SecuritiesPriceService priceService;

    @Mock
    private com.capitall.repository.WalletRepository walletRepository;

    @Mock
    private ExchangeRateService exchangeRateService;

    private SecurityPortfolioService portfolioService;

    @BeforeEach
    void setUp() {
        portfolioService = new SecurityPortfolioService(holdingRepository, tradeRepository, priceService, walletRepository, exchangeRateService);
        
        lenient().when(exchangeRateService.convert(any(BigDecimal.class), anyString(), anyString())).thenAnswer(invocation -> {
            BigDecimal amount = invocation.getArgument(0);
            String from = invocation.getArgument(1);
            String to = invocation.getArgument(2);
            if (from.equalsIgnoreCase(to)) return amount;
            if ("USD".equalsIgnoreCase(to) && "PLN".equalsIgnoreCase(from)) {
                return amount.multiply(new BigDecimal("0.25"));
            }
            if ("USD".equalsIgnoreCase(to) && "EUR".equalsIgnoreCase(from)) {
                return amount.multiply(new BigDecimal("1.10"));
            }
            return amount;
        });

        lenient().when(walletRepository.findByUserIdForUpdate(any(UUID.class))).thenAnswer(invocation -> {
            UUID uid = invocation.getArgument(0);
            return Optional.of(new com.capitall.model.Wallet(uid, new BigDecimal("10000.00")));
        });
        lenient().when(walletRepository.save(any(com.capitall.model.Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void executeTrade_ShouldCreateNewHolding_WhenBuyAndNoExistingHolding() {
        UUID userId = UUID.randomUUID();
        String symbol = "AAPL";
        String name = "Apple Inc.";
        BigDecimal shares = new BigDecimal("10");
        BigDecimal price = new BigDecimal("150");
        BigDecimal fee = new BigDecimal("5");
        String currency = "USD";

        when(holdingRepository.findByUserIdAndSymbol(userId, symbol)).thenReturn(Optional.empty());

        portfolioService.executeTrade(userId, SecurityTrade.Side.BUY, symbol, name, shares, price, fee, currency);

        verify(holdingRepository).save(argThat(holding -> {
            assertThat(holding.getUserId()).isEqualTo(userId);
            assertThat(holding.getSymbol()).isEqualTo(symbol);
            assertThat(holding.getName()).isEqualTo(name);
            assertThat(holding.getShares()).isEqualTo(shares);
            assertThat(holding.getAvgCost()).isEqualTo(new BigDecimal("150.5000")); // (10*150 + 5) / 10 = 150.50
            return true;
        }));

        verify(tradeRepository).save(any(SecurityTrade.class));
    }

    @Test
    void executeTrade_ShouldAverageCost_WhenBuyWithExistingHolding() {
        UUID userId = UUID.randomUUID();
        String symbol = "AAPL";
        String name = "Apple Inc.";
        BigDecimal existingShares = new BigDecimal("10");
        BigDecimal existingAvgCost = new BigDecimal("100.00");
        SecurityHolding existingHolding = new SecurityHolding(userId, symbol, name, "USD");
        existingHolding.setShares(existingShares);
        existingHolding.setAvgCost(existingAvgCost);

        BigDecimal buyShares = new BigDecimal("5");
        BigDecimal buyPrice = new BigDecimal("160");
        BigDecimal buyFee = new BigDecimal("10");

        when(holdingRepository.findByUserIdAndSymbol(userId, symbol)).thenReturn(Optional.of(existingHolding));

        portfolioService.executeTrade(userId, SecurityTrade.Side.BUY, symbol, name, buyShares, buyPrice, buyFee, "USD");

        // Expected AvgCost calculation:
        // currentTotalCost = 10 * 100 = 1000
        // tradeCost = 5 * 160 + 10 = 810
        // newShares = 15
        // newAvgCost = 1810 / 15 = 120.6667
        verify(holdingRepository).save(argThat(holding -> {
            assertThat(holding.getShares()).isEqualTo(new BigDecimal("15"));
            assertThat(holding.getAvgCost()).isEqualTo(new BigDecimal("120.6667"));
            return true;
        }));
    }

    @Test
    void executeTrade_ShouldThrowException_WhenSellAndNoExistingHolding() {
        UUID userId = UUID.randomUUID();
        String symbol = "AAPL";
        String name = "Apple Inc.";

        when(holdingRepository.findByUserIdAndSymbol(userId, symbol)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> portfolioService.executeTrade(userId, SecurityTrade.Side.SELL, symbol, name, BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO, "USD"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Nie posiadasz akcji tego waloru, aby móc je sprzedać.");
    }

    @Test
    void executeTrade_ShouldThrowException_WhenSellMoreSharesThanOwned() {
        UUID userId = UUID.randomUUID();
        String symbol = "AAPL";
        String name = "Apple Inc.";
        SecurityHolding existingHolding = new SecurityHolding(userId, symbol, name, "USD");
        existingHolding.setShares(new BigDecimal("5"));

        when(holdingRepository.findByUserIdAndSymbol(userId, symbol)).thenReturn(Optional.of(existingHolding));

        assertThatThrownBy(() -> portfolioService.executeTrade(userId, SecurityTrade.Side.SELL, symbol, name, new BigDecimal("6"), BigDecimal.TEN, BigDecimal.ZERO, "USD"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Niewystarczająca liczba akcji do sprzedaży.");
    }

    @Test
    void executeTrade_ShouldDeleteHolding_WhenSellAllShares() {
        UUID userId = UUID.randomUUID();
        String symbol = "AAPL";
        String name = "Apple Inc.";
        SecurityHolding existingHolding = new SecurityHolding(userId, symbol, name, "USD");
        existingHolding.setShares(new BigDecimal("10"));
        existingHolding.setAvgCost(new BigDecimal("100"));

        when(holdingRepository.findByUserIdAndSymbol(userId, symbol)).thenReturn(Optional.of(existingHolding));

        portfolioService.executeTrade(userId, SecurityTrade.Side.SELL, symbol, name, new BigDecimal("10"), new BigDecimal("120"), BigDecimal.ZERO, "USD");

        verify(holdingRepository).delete(existingHolding);
        verify(holdingRepository, never()).save(any(SecurityHolding.class));
        verify(tradeRepository).save(any(SecurityTrade.class));
    }

    @Test
    void executeTrade_ShouldDecreaseShares_WhenSellPartialShares() {
        UUID userId = UUID.randomUUID();
        String symbol = "AAPL";
        String name = "Apple Inc.";
        SecurityHolding existingHolding = new SecurityHolding(userId, symbol, name, "USD");
        existingHolding.setShares(new BigDecimal("10"));
        existingHolding.setAvgCost(new BigDecimal("100"));

        when(holdingRepository.findByUserIdAndSymbol(userId, symbol)).thenReturn(Optional.of(existingHolding));

        portfolioService.executeTrade(userId, SecurityTrade.Side.SELL, symbol, name, new BigDecimal("3"), new BigDecimal("120"), BigDecimal.ZERO, "USD");

        verify(holdingRepository).save(argThat(holding -> {
            assertThat(holding.getShares()).isEqualTo(new BigDecimal("7"));
            assertThat(holding.getAvgCost()).isEqualTo(new BigDecimal("100")); // avg cost shouldn't change on sell
            return true;
        }));
        verify(tradeRepository).save(any(SecurityTrade.class));
    }

    @Test
    void getPortfolioSummary_ShouldCalculateCorrectPnLAndRoi() {
        UUID userId = UUID.randomUUID();
        SecurityHolding h1 = new SecurityHolding(userId, "AAPL", "Apple Inc.", "USD");
        h1.setShares(new BigDecimal("10"));
        h1.setAvgCost(new BigDecimal("150"));

        SecurityHolding h2 = new SecurityHolding(userId, "TSLA", "Tesla Inc.", "USD");
        h2.setShares(new BigDecimal("5"));
        h2.setAvgCost(new BigDecimal("200"));

        when(holdingRepository.findByUserId(userId)).thenReturn(List.of(h1, h2));
        when(priceService.getCurrentPrice("AAPL")).thenReturn(new BigDecimal("160"));
        when(priceService.getCurrentPrice("TSLA")).thenReturn(new BigDecimal("190"));

        SecurityPortfolioService.SecurityPortfolioSummary summary = portfolioService.getPortfolioSummary(userId);

        // AAPL totalCost = 10 * 150 = 1500, value = 10 * 160 = 1600. PnL = +100
        // TSLA totalCost = 5 * 200 = 1000, value = 5 * 190 = 950. PnL = -50
        // Overall totalCost = 2500, totalValue = 2550
        // totalPnL = +50
        // totalRoi = 50 * 100 / 2500 = 2.00%
        assertThat(summary.getTotalCost()).isEqualTo(new BigDecimal("2500.00"));
        assertThat(summary.getTotalValue()).isEqualTo(new BigDecimal("2550.00"));
        assertThat(summary.getTotalPnL()).isEqualTo(new BigDecimal("50.00"));
        assertThat(summary.getTotalRoi()).isEqualTo(new BigDecimal("2.00"));
    }

    @Test
    void executeTrade_ShouldThrowException_WhenWalletHasInsufficientFunds() {
        UUID userId = UUID.randomUUID();
        String symbol = "AAPL";
        String name = "Apple Inc.";
        BigDecimal shares = new BigDecimal("10");
        BigDecimal price = new BigDecimal("150");
        BigDecimal fee = new BigDecimal("5");
        String currency = "USD";

        // Wallet with only $1000
        com.capitall.model.Wallet wallet = new com.capitall.model.Wallet(userId, new BigDecimal("1000.00"));
        when(walletRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(wallet));

        assertThatThrownBy(() -> portfolioService.executeTrade(userId, SecurityTrade.Side.BUY, symbol, name, shares, price, fee, currency))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Niewystarczające środki w portfelu");
    }

    @Test
    void executeTrade_ShouldDeductWallet_WhenBuyIsSuccessful() {
        UUID userId = UUID.randomUUID();
        String symbol = "AAPL";
        String name = "Apple Inc.";
        BigDecimal shares = new BigDecimal("10");
        BigDecimal price = new BigDecimal("150");
        BigDecimal fee = new BigDecimal("5");
        String currency = "USD";

        // Wallet with $2000
        com.capitall.model.Wallet wallet = new com.capitall.model.Wallet(userId, new BigDecimal("2000.00"));
        when(walletRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(wallet));
        when(holdingRepository.findByUserIdAndSymbol(userId, symbol)).thenReturn(Optional.empty());

        portfolioService.executeTrade(userId, SecurityTrade.Side.BUY, symbol, name, shares, price, fee, currency);

        // $2000 - ($1500 + $5) = $495
        assertThat(wallet.getUsdBalance()).isEqualTo(new BigDecimal("495.00"));
        verify(walletRepository).save(wallet);
    }
}
