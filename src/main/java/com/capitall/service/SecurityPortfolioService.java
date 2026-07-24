package com.capitall.service;

import com.capitall.model.SecurityHolding;
import com.capitall.model.SecurityTrade;
import com.capitall.model.Wallet;
import com.capitall.repository.SecurityHoldingRepository;
import com.capitall.repository.SecurityTradeRepository;
import com.capitall.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class SecurityPortfolioService {

    private final SecurityHoldingRepository holdingRepository;
    private final SecurityTradeRepository tradeRepository;
    private final SecuritiesPriceService priceService;
    private final WalletRepository walletRepository;
    private final ExchangeRateService exchangeRateService;

    public SecurityPortfolioService(SecurityHoldingRepository holdingRepository,
                                    SecurityTradeRepository tradeRepository,
                                    SecuritiesPriceService priceService,
                                    WalletRepository walletRepository,
                                    ExchangeRateService exchangeRateService) {
        this.holdingRepository = holdingRepository;
        this.tradeRepository = tradeRepository;
        this.priceService = priceService;
        this.walletRepository = walletRepository;
        this.exchangeRateService = exchangeRateService;
    }

    @Transactional
    public void executeTrade(UUID userId, SecurityTrade.Side side, String symbol, String name,
                             BigDecimal shares, BigDecimal price, BigDecimal fee, String currency) {
        if (shares.compareTo(BigDecimal.ZERO) <= 0 || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Ilość i cena muszą być większe od zera.");
        }
        if (fee.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Prowizja nie może być ujemna.");
        }

        String cleanSymbol = symbol.toUpperCase().trim();
        String cleanCurrency = currency.toUpperCase().trim();
        Optional<SecurityHolding> optHolding = holdingRepository.findByUserIdAndSymbol(userId, cleanSymbol);

        BigDecimal gross;
        if (side == SecurityTrade.Side.BUY) {
            gross = shares.multiply(price).add(fee);
        } else {
            gross = shares.multiply(price).subtract(fee);
        }

        Wallet wallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseGet(() -> walletRepository.save(new Wallet(userId, new BigDecimal("1500.00"))));

        BigDecimal currentBalance = wallet.getBalance(cleanCurrency);

        if (side == SecurityTrade.Side.BUY) {
            if (currentBalance.compareTo(gross) < 0) {
                throw new IllegalArgumentException("Niewystarczające środki w portfelu (" + cleanCurrency + "). Dostępne: "
                        + currentBalance.setScale(2, RoundingMode.HALF_UP) + " " + cleanCurrency
                        + ", wymagane: " + gross.setScale(2, RoundingMode.HALF_UP) + " " + cleanCurrency);
            }
            wallet.setBalance(cleanCurrency, currentBalance.subtract(gross).setScale(2, RoundingMode.HALF_UP));
        } else {
            wallet.setBalance(cleanCurrency, currentBalance.add(gross).setScale(2, RoundingMode.HALF_UP));
        }
        walletRepository.save(wallet);

        SecurityHolding holding;
        if (optHolding.isPresent()) {
            holding = optHolding.get();
        } else {
            if (side == SecurityTrade.Side.SELL) {
                throw new IllegalArgumentException("Nie posiadasz akcji tego waloru, aby móc je sprzedać.");
            }
            holding = new SecurityHolding(userId, cleanSymbol, name, cleanCurrency);
        }

        if (side == SecurityTrade.Side.BUY) {
            BigDecimal currentTotalCost = holding.getShares().multiply(holding.getAvgCost());
            BigDecimal tradeCost = shares.multiply(price).add(fee);
            BigDecimal newShares = holding.getShares().add(shares);
            BigDecimal newAvgCost = currentTotalCost.add(tradeCost)
                    .divide(newShares, 4, RoundingMode.HALF_UP);

            holding.setShares(newShares);
            holding.setAvgCost(newAvgCost);
            holdingRepository.save(holding);
        } else {
            if (holding.getShares().compareTo(shares) < 0) {
                throw new IllegalArgumentException("Niewystarczająca liczba akcji do sprzedaży. Posiadasz: "
                        + holding.getShares() + ", próbujesz sprzedać: " + shares);
            }
            BigDecimal newShares = holding.getShares().subtract(shares);
            if (newShares.compareTo(BigDecimal.ZERO) == 0) {
                holdingRepository.delete(holding);
            } else {
                holding.setShares(newShares);
                holdingRepository.save(holding);
            }
        }

        SecurityTrade trade = new SecurityTrade();
        trade.setUserId(userId);
        trade.setSymbol(cleanSymbol);
        trade.setName(name);
        trade.setSide(side);
        trade.setShares(shares);
        trade.setPrice(price);
        trade.setFee(fee);
        trade.setGross(gross);
        trade.setCurrency(cleanCurrency);
        trade.setCreatedAt(LocalDateTime.now());
        tradeRepository.save(trade);
    }

    public List<SecurityPortfolioItem> getPortfolioItems(UUID userId) {
        List<SecurityHolding> holdings = holdingRepository.findByUserId(userId);
        return holdings.parallelStream()
                .map(h -> {
                    BigDecimal currentPrice = priceService.getCurrentPrice(h.getSymbol());
                    if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
                        currentPrice = h.getAvgCost();
                    }
                    return new SecurityPortfolioItem(h, currentPrice, exchangeRateService);
                })
                .collect(java.util.stream.Collectors.toList());
    }

    public SecurityPortfolioSummary getPortfolioSummary(UUID userId) {
        List<SecurityPortfolioItem> items = getPortfolioItems(userId);
        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalValue = BigDecimal.ZERO;

        for (SecurityPortfolioItem item : items) {
            totalCost = totalCost.add(item.getTotalCost());
            totalValue = totalValue.add(item.getCurrentValue());
        }

        BigDecimal totalPnL = totalValue.subtract(totalCost);
        BigDecimal totalRoi = BigDecimal.ZERO;
        if (totalCost.compareTo(BigDecimal.ZERO) > 0) {
            totalRoi = totalPnL.multiply(new BigDecimal("100"))
                    .divide(totalCost, 2, RoundingMode.HALF_UP);
        }

        return new SecurityPortfolioSummary(totalCost, totalValue, totalPnL, totalRoi);
    }

    public List<SecurityTrade> getTradeHistory(UUID userId) {
        return tradeRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public static class SecurityPortfolioItem {
        private final SecurityHolding holding;
        private final BigDecimal currentPrice;
        private final BigDecimal currentValue;
        private final BigDecimal totalCost;
        private final BigDecimal unrealizedPnL;
        private final BigDecimal unrealizedPnLPct;

        public SecurityPortfolioItem(SecurityHolding holding, BigDecimal currentPrice, ExchangeRateService exchangeRateService) {
            this.holding = holding;
            this.currentPrice = currentPrice;

            BigDecimal rawCurrentValue = holding.getShares().multiply(currentPrice);
            BigDecimal rawTotalCost = holding.getShares().multiply(holding.getAvgCost());

            this.currentValue = exchangeRateService.convert(rawCurrentValue, holding.getCurrency(), "USD").setScale(2, RoundingMode.HALF_UP);
            this.totalCost = exchangeRateService.convert(rawTotalCost, holding.getCurrency(), "USD").setScale(2, RoundingMode.HALF_UP);
            this.unrealizedPnL = this.currentValue.subtract(this.totalCost);

            BigDecimal pnlPct = BigDecimal.ZERO;
            if (this.totalCost.compareTo(BigDecimal.ZERO) > 0) {
                pnlPct = this.unrealizedPnL.multiply(new BigDecimal("100"))
                        .divide(this.totalCost, 2, RoundingMode.HALF_UP);
            }
            this.unrealizedPnLPct = pnlPct;
        }

        public SecurityHolding getHolding() { return holding; }
        public BigDecimal getCurrentPrice() { return currentPrice; }
        public BigDecimal getCurrentValue() { return currentValue; }
        public BigDecimal getTotalCost() { return totalCost; }
        public BigDecimal getUnrealizedPnL() { return unrealizedPnL; }
        public BigDecimal getUnrealizedPnLPct() { return unrealizedPnLPct; }
    }

    public static class SecurityPortfolioSummary {
        private final BigDecimal totalCost;
        private final BigDecimal totalValue;
        private final BigDecimal totalPnL;
        private final BigDecimal totalRoi;

        public SecurityPortfolioSummary(BigDecimal totalCost, BigDecimal totalValue, BigDecimal totalPnL, BigDecimal totalRoi) {
            this.totalCost = totalCost;
            this.totalValue = totalValue;
            this.totalPnL = totalPnL;
            this.totalRoi = totalRoi;
        }

        public BigDecimal getTotalCost() { return totalCost; }
        public BigDecimal getTotalValue() { return totalValue; }
        public BigDecimal getTotalPnL() { return totalPnL; }
        public BigDecimal getTotalRoi() { return totalRoi; }
    }
}
