package com.capitall.service;

import com.capitall.model.SimulatedHolding;
import com.capitall.model.SimulatedTrade;
import com.capitall.model.SimulatedWallet;
import com.capitall.repository.SimulatedHoldingRepository;
import com.capitall.repository.SimulatedTradeRepository;
import com.capitall.repository.SimulatedWalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class SimulatorService {

    private final SimulatedWalletRepository walletRepository;
    private final SimulatedHoldingRepository holdingRepository;
    private final SimulatedTradeRepository tradeRepository;
    private final SecuritiesPriceService priceService;

    public SimulatorService(SimulatedWalletRepository walletRepository,
                            SimulatedHoldingRepository holdingRepository,
                            SimulatedTradeRepository tradeRepository,
                            SecuritiesPriceService priceService) {
        this.walletRepository = walletRepository;
        this.holdingRepository = holdingRepository;
        this.tradeRepository = tradeRepository;
        this.priceService = priceService;
    }

    @Transactional
    public SimulatedWallet getOrCreateWallet(UUID userId) {
        return walletRepository.findByUserId(userId)
                .orElseGet(() -> walletRepository.save(new SimulatedWallet(userId, new BigDecimal("10000.00"))));
    }

    @Transactional
    public void resetPortfolio(UUID userId) {
        SimulatedWallet wallet = getOrCreateWallet(userId);
        wallet.setBalance(new BigDecimal("10000.00"));
        walletRepository.save(wallet);

        List<SimulatedHolding> holdings = holdingRepository.findByUserId(userId);
        holdingRepository.deleteAll(holdings);

        List<SimulatedTrade> trades = tradeRepository.findByUserIdOrderByTimestampDesc(userId);
        tradeRepository.deleteAll(trades);
    }

    @Transactional
    public void topUpWallet(UUID userId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Kwota doładowania musi być większa od zera.");
        }
        SimulatedWallet wallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseGet(() -> walletRepository.save(new SimulatedWallet(userId, new BigDecimal("10000.00"))));
        wallet.setBalance(wallet.getBalance().add(amount).setScale(2, RoundingMode.HALF_UP));
        walletRepository.save(wallet);
    }

    @Transactional
    public void executeTrade(UUID userId, SimulatedTrade.Side side, String symbol, BigDecimal shares) {
        if (shares.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Liczba jednostek musi być większa od zera.");
        }

        String cleanSymbol = symbol.toUpperCase().trim();
        BigDecimal price = priceService.getCurrentPrice(cleanSymbol);
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Nie udało się pobrać aktualnej ceny dla symbolu: " + cleanSymbol);
        }

        String name = getSymbolName(cleanSymbol);

        BigDecimal totalCost = shares.multiply(price);

        SimulatedWallet wallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseGet(() -> walletRepository.save(new SimulatedWallet(userId, new BigDecimal("10000.00"))));

        if (side == SimulatedTrade.Side.BUY) {
            if (wallet.getBalance().compareTo(totalCost) < 0) {
                throw new IllegalArgumentException("Niewystarczające wirtualne środki w portfelu. Posiadasz: $"
                        + wallet.getBalance().setScale(2, RoundingMode.HALF_UP) + ", wymagane: $"
                        + totalCost.setScale(2, RoundingMode.HALF_UP));
            }
            wallet.setBalance(wallet.getBalance().subtract(totalCost).setScale(2, RoundingMode.HALF_UP));
            walletRepository.save(wallet);

            SimulatedHolding holding = holdingRepository.findByUserIdAndSymbol(userId, cleanSymbol)
                    .orElseGet(() -> new SimulatedHolding(userId, cleanSymbol, name));

            BigDecimal currentTotalCost = holding.getShares().multiply(holding.getAvgCost());
            BigDecimal newShares = holding.getShares().add(shares);
            BigDecimal newAvgCost = currentTotalCost.add(totalCost).divide(newShares, 4, RoundingMode.HALF_UP);

            holding.setShares(newShares);
            holding.setAvgCost(newAvgCost);
            holdingRepository.save(holding);

        } else {
            SimulatedHolding holding = holdingRepository.findByUserIdAndSymbol(userId, cleanSymbol)
                    .orElseThrow(() -> new IllegalArgumentException("Nie posiadasz tego aktywa w swoim portfelu demo."));

            if (holding.getShares().compareTo(shares) < 0) {
                throw new IllegalArgumentException("Posiadasz niewystarczającą ilość jednostek do sprzedaży. Masz: "
                        + holding.getShares() + ", próbujesz sprzedać: " + shares);
            }

            wallet.setBalance(wallet.getBalance().add(totalCost).setScale(2, RoundingMode.HALF_UP));
            walletRepository.save(wallet);

            BigDecimal newShares = holding.getShares().subtract(shares);
            if (newShares.compareTo(BigDecimal.ZERO) == 0) {
                holdingRepository.delete(holding);
            } else {
                holding.setShares(newShares);
                holdingRepository.save(holding);
            }
        }

        SimulatedTrade trade = new SimulatedTrade(userId, side, cleanSymbol, name, shares, price);
        tradeRepository.save(trade);
    }

    public List<SimulatedHoldingSummary> getHoldingsSummary(UUID userId) {
        List<SimulatedHolding> holdings = holdingRepository.findByUserId(userId);
        return holdings.parallelStream()
                .map(h -> {
                    BigDecimal currentPrice = priceService.getCurrentPrice(h.getSymbol());
                    return new SimulatedHoldingSummary(h, currentPrice);
                })
                .collect(java.util.stream.Collectors.toList());
    }

    public List<SimulatedTrade> getTradeHistory(UUID userId) {
        return tradeRepository.findByUserIdOrderByTimestampDesc(userId);
    }

    public BigDecimal getPrice(String symbol) {
        return priceService.getCurrentPrice(symbol);
    }

    public Map<String, BigDecimal> getBatchPrices(List<String> symbols) {
        return priceService.getBatchPrices(symbols);
    }

    private String getSymbolName(String symbol) {
        Map<String, String> popular = new HashMap<>();
        popular.put("AAPL", "Apple Inc.");
        popular.put("TSLA", "Tesla Inc.");
        popular.put("MSFT", "Microsoft Corp.");
        popular.put("NVDA", "NVIDIA Corp.");
        popular.put("CDR", "CD Projekt SA");
        popular.put("PKO", "PKO BP SA");
        popular.put("SPY", "SPDR S&P 500 ETF");
        popular.put("QQQ", "Invesco QQQ Trust");
        popular.put("BABA", "Alibaba Group");
        popular.put("NIO", "Nio Inc.");
        popular.put("LPP", "LPP SA");
        popular.put("PKN", "Orlen");
        popular.put("KGH", "KGHM");
        popular.put("BTC-USD", "Bitcoin");
        popular.put("ETH-USD", "Ethereum");
        popular.put("SOL-USD", "Solana");
        popular.put("ADA-USD", "Cardano");
        popular.put("DOT-USD", "Polkadot");
        popular.put("XRP-USD", "Ripple");
        popular.put("DOGE-USD", "Dogecoin");
        popular.put("LTC-USD", "Litecoin");
        popular.put("LINK-USD", "Chainlink");
        popular.put("BNB-USD", "Binance Coin");
        popular.put("AVAX-USD", "Avalanche");
        popular.put("POL-USD", "Polygon");
        popular.put("SHIB-USD", "Shiba Inu");

        return popular.getOrDefault(symbol, symbol);
    }

    public static class SimulatedHoldingSummary {
        private final SimulatedHolding holding;
        private final BigDecimal currentPrice;
        private final BigDecimal currentValue;
        private final BigDecimal totalCost;
        private final BigDecimal pnl;
        private final BigDecimal roi;

        public SimulatedHoldingSummary(SimulatedHolding holding, BigDecimal currentPrice) {
            this.holding = holding;
            this.currentPrice = currentPrice != null ? currentPrice : BigDecimal.ZERO;
            this.currentValue = holding.getShares().multiply(this.currentPrice).setScale(2, RoundingMode.HALF_UP);
            this.totalCost = holding.getShares().multiply(holding.getAvgCost()).setScale(2, RoundingMode.HALF_UP);
            this.pnl = this.currentValue.subtract(this.totalCost).setScale(2, RoundingMode.HALF_UP);

            if (this.totalCost.compareTo(BigDecimal.ZERO) > 0) {
                this.roi = this.pnl.multiply(new BigDecimal("100")).divide(this.totalCost, 2, RoundingMode.HALF_UP);
            } else {
                this.roi = BigDecimal.ZERO;
            }
        }

        public SimulatedHolding getHolding() { return holding; }
        public BigDecimal getCurrentPrice() { return currentPrice; }
        public BigDecimal getCurrentValue() { return currentValue; }
        public BigDecimal getTotalCost() { return totalCost; }
        public BigDecimal getPnl() { return pnl; }
        public BigDecimal getRoi() { return roi; }
    }
}
