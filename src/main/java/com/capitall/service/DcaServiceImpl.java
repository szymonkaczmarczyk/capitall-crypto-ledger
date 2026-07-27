package com.capitall.service;

import com.capitall.model.RecurringOrder;
import com.capitall.repository.RecurringOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class DcaServiceImpl implements DcaService {

    private static final Logger log = LoggerFactory.getLogger(DcaServiceImpl.class);

    private final RecurringOrderRepository recurringOrderRepository;
    private final DcaOrderProcessor dcaOrderProcessor;

    public DcaServiceImpl(RecurringOrderRepository recurringOrderRepository,
                          DcaOrderProcessor dcaOrderProcessor) {
        this.recurringOrderRepository = recurringOrderRepository;
        this.dcaOrderProcessor = dcaOrderProcessor;
    }

    @Override
    @Transactional
    public RecurringOrder createOrder(UUID userId, String symbol, BigDecimal usdAmount, Integer intervalDays, Boolean smartDca) {
        if (usdAmount == null || usdAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Kwota zlecenia musi być większa od zera.");
        }
        if (intervalDays == null || intervalDays < 1) {
            throw new IllegalArgumentException("Interwał dni musi wynosić co najmniej 1 dzień.");
        }
        if (symbol == null || symbol.trim().isEmpty()) {
            throw new IllegalArgumentException("Symbol aktywa jest wymagany.");
        }

        String cleanSymbol = symbol.trim().toUpperCase();
        
        // Initial execution starts immediately
        LocalDateTime now = LocalDateTime.now();
        RecurringOrder order = new RecurringOrder(userId, cleanSymbol, usdAmount, intervalDays, now);
        order.setSmartDca(smartDca != null ? smartDca : false);
        
        return recurringOrderRepository.save(order);
    }

    @Override
    public List<RecurringOrder> getUserOrders(UUID userId) {
        return recurringOrderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    @Transactional
    public void deleteOrder(UUID userId, UUID orderId) {
        RecurringOrder order = recurringOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono zlecenia cyklicznego."));
        if (!order.getUserId().equals(userId)) {
            throw new IllegalStateException("Brak uprawnień do edycji tego zlecenia.");
        }
        recurringOrderRepository.delete(order);
    }

    @Override
    @Transactional
    public RecurringOrder toggleOrder(UUID userId, UUID orderId) {
        RecurringOrder order = recurringOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono zlecenia cyklicznego."));
        if (!order.getUserId().equals(userId)) {
            throw new IllegalStateException("Brak uprawnień do edycji tego zlecenia.");
        }
        order.setActive(!order.getActive());
        return recurringOrderRepository.save(order);
    }

    @Override
    public void executeDueOrders() {
        LocalDateTime now = LocalDateTime.now();
        List<RecurringOrder> dueOrders = recurringOrderRepository.findByActiveTrueAndNextExecutionLessThanEqual(now);

        if (dueOrders.isEmpty()) {
            return;
        }

        log.info("Processing {} due DCA recurring orders...", dueOrders.size());

        for (RecurringOrder order : dueOrders) {
            try {
                // Execute each order in an isolated transaction
                dcaOrderProcessor.processSingleOrder(order.getId());
            } catch (Exception e) {
                log.error("Failed to process DCA order {} for user {}: {}",
                        order.getId(), order.getUserId(), e.getMessage());
            }
        }
    }
}
