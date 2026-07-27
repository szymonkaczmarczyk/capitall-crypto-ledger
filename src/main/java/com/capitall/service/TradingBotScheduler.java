package com.capitall.service;

import com.capitall.model.SimulatedHolding;
import com.capitall.repository.SimulatedHoldingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class TradingBotScheduler {

    private static final Logger log = LoggerFactory.getLogger(TradingBotScheduler.class);

    private final SimulatedHoldingRepository holdingRepository;
    private final SecuritiesPriceService priceService;
    private final WalletService walletService;

    public TradingBotScheduler(SimulatedHoldingRepository holdingRepository,
                               SecuritiesPriceService priceService,
                               WalletService walletService) {
        this.holdingRepository = holdingRepository;
        this.priceService = priceService;
        this.walletService = walletService;
    }

    @Scheduled(fixedRate = 60000) // Co 60 sekund
    @Transactional
    public void processTrailingStops() {
        List<SimulatedHolding> holdings = holdingRepository.findByTrailingStopEnabledTrue();
        if (holdings.isEmpty()) return;

        log.info("Checking Trailing Stop-Loss for {} holdings", holdings.size());

        for (SimulatedHolding holding : holdings) {
            try {
                BigDecimal currentPrice = priceService.getCurrentPrice(holding.getSymbol());
                if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                BigDecimal highestPrice = holding.getHighestPriceReached();
                if (highestPrice == null) {
                    highestPrice = currentPrice;
                    holding.setHighestPriceReached(highestPrice);
                    holdingRepository.save(holding);
                    continue;
                }

                if (currentPrice.compareTo(highestPrice) > 0) {
                    // New high, update the highest price
                    holding.setHighestPriceReached(currentPrice);
                    holdingRepository.save(holding);
                    log.info("Trailing Stop updated for {} (User {}). New highest price: {}", 
                             holding.getSymbol(), holding.getUserId(), currentPrice);
                } else {
                    // Check if price dropped below stop percent
                    BigDecimal dropPercent = holding.getTrailingStopPercent();
                    if (dropPercent != null) {
                        BigDecimal multiplier = BigDecimal.ONE.subtract(dropPercent.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
                        BigDecimal stopPrice = highestPrice.multiply(multiplier);
                        
                        if (currentPrice.compareTo(stopPrice) <= 0) {
                            // Trailing Stop triggered!
                            log.warn("Trailing Stop TRIGGERED for {} (User {}). Highest: {}, Current: {}, Stop: {}. Selling all shares.",
                                     holding.getSymbol(), holding.getUserId(), highestPrice, currentPrice, stopPrice);
                            
                            // Disable trailing stop
                            holding.setTrailingStopEnabled(false);
                            holdingRepository.save(holding);
                            
                            // Sell all shares
                            walletService.sell(holding.getUserId(), holding.getSymbol(), currentPrice, holding.getShares());
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Failed to process Trailing Stop for holding ID {}: {}", holding.getId(), e.getMessage());
            }
        }
    }
}
