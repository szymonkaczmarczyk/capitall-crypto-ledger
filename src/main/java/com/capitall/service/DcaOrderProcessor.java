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

        // Execute trade via WalletService inside this isolated transaction
        walletService.buy(order.getUserId(), order.getSymbol(), price, order.getUsdAmount());

        order.setLastExecution(now);
        order.setNextExecution(now.plusDays(order.getIntervalDays()));
        recurringOrderRepository.save(order);

        log.info("Zlecenie DCA {} dla użytkownika {} wykonane pomyślnie. Zakupiono {} za ${}",
                orderId, order.getUserId(), order.getSymbol(), order.getUsdAmount());
    }
}
