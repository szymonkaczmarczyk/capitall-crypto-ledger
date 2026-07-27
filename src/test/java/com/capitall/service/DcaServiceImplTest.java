package com.capitall.service;

import com.capitall.model.RecurringOrder;
import com.capitall.repository.RecurringOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class DcaServiceImplTest {

    private RecurringOrderRepository recurringOrderRepository;
    private DcaOrderProcessor dcaOrderProcessor;
    private DcaServiceImpl dcaService;

    private UUID userId;

    @BeforeEach
    public void setUp() {
        recurringOrderRepository = mock(RecurringOrderRepository.class);
        dcaOrderProcessor = mock(DcaOrderProcessor.class);

        dcaService = new DcaServiceImpl(recurringOrderRepository, dcaOrderProcessor);
        userId = UUID.randomUUID();
    }

    @Test
    public void testCreateOrder_Success() {
        when(recurringOrderRepository.save(any(RecurringOrder.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RecurringOrder order = dcaService.createOrder(userId, "btc", new BigDecimal("50.00"), 7, false);

        assertNotNull(order);
        assertEquals(userId, order.getUserId());
        assertEquals("BTC", order.getSymbol());
        assertEquals(new BigDecimal("50.00"), order.getUsdAmount());
        assertEquals(7, order.getIntervalDays());
        assertTrue(order.getActive());
        assertNotNull(order.getNextExecution());
    }

    @Test
    public void testCreateOrder_InvalidAmount_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            dcaService.createOrder(userId, "BTC", new BigDecimal("0.00"), 7, false);
        });
    }

    @Test
    public void testExecuteDueOrders_Success() {
        LocalDateTime now = LocalDateTime.now();
        RecurringOrder order = new RecurringOrder(userId, "BTC", new BigDecimal("50.00"), 7, now.minusMinutes(5));
        UUID orderId = UUID.randomUUID();
        order.setId(orderId);

        when(recurringOrderRepository.findByActiveTrueAndNextExecutionLessThanEqual(any(LocalDateTime.class)))
                .thenReturn(List.of(order));

        dcaService.executeDueOrders();

        verify(dcaOrderProcessor).processSingleOrder(orderId);
    }
}
