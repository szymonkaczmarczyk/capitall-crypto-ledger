package com.capitall.service;

import com.capitall.model.RecurringOrder;
import com.capitall.repository.RecurringOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class DcaOrderProcessor {

    private static final Logger log = LoggerFactory.getLogger(DcaOrderProcessor.class);

    private final RecurringOrderRepository recurringOrderRepository;
    private final WalletService walletService;
    private final SecuritiesPriceService priceService;

    public DcaOrderProcessor(RecurringOrderRepository recurringOrderRepository,
                             WalletService walletService,
                             SecuritiesPriceService priceService) {
        this.recurringOrderRepository = recurringOrderRepository;
        this.walletService = walletService;
        this.priceService = priceService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processSingleOrder(UUID orderId) {
        RecurringOrder order = recurringOrderRepository.findById(orderId).orElse(null);
        if (order == null || !Boolean.TRUE.equals(order.getActive())) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        BigDecimal price = priceService.getCurrentPrice(order.getSymbol());
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Brak ceny rynkowej dla aktywa {}. Pomijanie zlecenia DCA {}.", order.getSymbol(), orderId);
            return;
        }

        BigDecimal finalAmount = order.getUsdAmount();
        if (Boolean.TRUE.equals(order.getSmartDca())) {
            try {
                java.util.List<com.capitall.dto.BacktestResult.OHLCVBar> bars = priceService.fetchOHLCV(order.getSymbol(), "7d", "1d");
                if (bars != null && !bars.isEmpty()) {
                    double price7DaysAgo = bars.get(0).close;
                    if (price7DaysAgo > 0) {
                        double dropThreshold = price7DaysAgo * 0.90;
                        if (price.doubleValue() <= dropThreshold) {
                            BigDecimal multiplier = order.getSmartMultiplier() != null ? order.getSmartMultiplier() : BigDecimal.valueOf(2.0);
                            finalAmount = finalAmount.multiply(multiplier);
                            log.info("Smart DCA activated for {}: Price dropped from {} to {}. Multiplying amount by {} to {}",
                                     order.getSymbol(), price7DaysAgo, price, multiplier, finalAmount);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to apply Smart DCA logic for {}: {}", order.getSymbol(), e.getMessage());
            }
        }

        // Execute trade via WalletService inside this isolated transaction
        walletService.buy(order.getUserId(), order.getSymbol(), price, finalAmount);

        order.setLastExecution(now);
        order.setNextExecution(now.plusDays(order.getIntervalDays()));
        recurringOrderRepository.save(order);

        log.info("Zlecenie DCA {} dla użytkownika {} wykonane pomyślnie. Zakupiono {} za ${}",
                orderId, order.getUserId(), order.getSymbol(), order.getUsdAmount());
    }
}
