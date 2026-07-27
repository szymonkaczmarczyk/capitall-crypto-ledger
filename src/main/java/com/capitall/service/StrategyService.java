package com.capitall.service;

import com.capitall.model.TradingStrategy;
import com.capitall.repository.TradingStrategyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class StrategyService {

    private static final Logger log = LoggerFactory.getLogger(StrategyService.class);

    private final TradingStrategyRepository strategyRepository;
    private final SecuritiesPriceService priceService;
    private final WalletService walletService;

    public StrategyService(TradingStrategyRepository strategyRepository,
                           SecuritiesPriceService priceService,
                           WalletService walletService) {
        this.strategyRepository = strategyRepository;
        this.priceService = priceService;
        this.walletService = walletService;
    }

    public List<TradingStrategy> getUserStrategies(UUID userId) {
        List<TradingStrategy> list = strategyRepository.findByUserIdOrderByCreatedAtDesc(userId);
        if (list.isEmpty()) {
            // Seed default demonstration bots for new user
            TradingStrategy b1 = new TradingStrategy(
                    userId, "Smart DCA - NVDA Tech Leader", "NVDA", "Binance API",
                    TradingStrategy.TriggerType.SMART_DIP, new BigDecimal("150.00"), new BigDecimal("3.50")
            );
            b1.setDipPercentage(new BigDecimal("4.00"));
            b1.setTotalTradesCount(12);
            b1.setTotalProfitUsd(new BigDecimal("142.80"));
            b1.setLastExecutedAt(LocalDateTime.now().minusHours(4));
            strategyRepository.save(b1);

            TradingStrategy b2 = new TradingStrategy(
                    userId, "RSI Oversold Dip Buyer - BTC", "BTC-USD", "Capitall Simulator",
                    TradingStrategy.TriggerType.RSI_OVERSOLD, new BigDecimal("250.00"), new BigDecimal("4.00")
            );
            b2.setTakeProfitPercent(new BigDecimal("12.00"));
            b2.setTotalTradesCount(8);
            b2.setTotalProfitUsd(new BigDecimal("310.50"));
            b2.setLastExecutedAt(LocalDateTime.now().minusDays(1));
            strategyRepository.save(b2);

            list = strategyRepository.findByUserIdOrderByCreatedAtDesc(userId);
        }
        return list;
    }

    @Transactional
    public TradingStrategy saveStrategy(TradingStrategy strategy) {
        return strategyRepository.save(strategy);
    }

    @Transactional
    public void toggleStrategy(UUID strategyId, UUID userId) {
        strategyRepository.findById(strategyId).ifPresent(strat -> {
            if (strat.getUserId().equals(userId)) {
                strat.setActive(!strat.isActive());
                strategyRepository.save(strat);
            }
        });
    }

    @Transactional
    public void deleteStrategy(UUID strategyId, UUID userId) {
        strategyRepository.findById(strategyId).ifPresent(strat -> {
            if (strat.getUserId().equals(userId)) {
                strategyRepository.delete(strat);
            }
        });
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void executeActiveStrategies() {
        List<TradingStrategy> activeBots = strategyRepository.findByActiveTrue();
        if (activeBots.isEmpty()) return;

        for (TradingStrategy bot : activeBots) {
            try {
                BigDecimal currentPrice = priceService.getCurrentPrice(bot.getSymbol());
                if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) <= 0) continue;

                boolean shouldExecute = false;
                if (bot.getTriggerType() == TradingStrategy.TriggerType.DCA_SCHEDULED) {
                    if (bot.getLastExecutedAt() == null || bot.getLastExecutedAt().plusMinutes(bot.getIntervalMinutes()).isBefore(LocalDateTime.now())) {
                        shouldExecute = true;
                    }
                } else if (bot.getTriggerType() == TradingStrategy.TriggerType.SMART_DIP) {
                    // Simulating dip trigger condition
                    if (Math.random() < 0.15) shouldExecute = true;
                } else {
                    if (Math.random() < 0.10) shouldExecute = true;
                }

                if (shouldExecute) {
                    BigDecimal sharesToBuy = bot.getTradeAmountUsd().divide(currentPrice, 6, java.math.RoundingMode.HALF_UP);
                    if (sharesToBuy.compareTo(BigDecimal.ZERO) > 0) {
                        walletService.buy(bot.getUserId(), bot.getSymbol(), currentPrice, sharesToBuy);
                        bot.setTotalTradesCount(bot.getTotalTradesCount() + 1);
                        bot.setLastExecutedAt(LocalDateTime.now());
                        bot.setTotalProfitUsd(bot.getTotalProfitUsd().add(bot.getTradeAmountUsd().multiply(new BigDecimal("0.025"))));
                        strategyRepository.save(bot);
                        log.info("[StrategyBot] Wykonano automatyczną transakcję bota: {} dla symulacji {}", bot.getName(), bot.getSymbol());
                    }
                }
            } catch (Exception e) {
                log.debug("[StrategyBot] Błąd bota {}: {}", bot.getName(), e.getMessage());
            }
        }
    }
}
