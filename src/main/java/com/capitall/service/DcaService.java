package com.capitall.service;

import com.capitall.model.RecurringOrder;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface DcaService {
    RecurringOrder createOrder(UUID userId, String symbol, BigDecimal usdAmount, Integer intervalDays, Boolean smartDca);
    List<RecurringOrder> getUserOrders(UUID userId);
    void deleteOrder(UUID userId, UUID orderId);
    RecurringOrder toggleOrder(UUID userId, UUID orderId);
    void executeDueOrders();
}
