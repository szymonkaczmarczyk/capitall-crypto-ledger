package com.capitall.service;

import com.capitall.model.PriceAlert;
import com.capitall.repository.PriceAlertRepository;
import com.capitall.repository.PriceAlertLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PriceAlertServiceTest {

    private PriceAlertRepository alertRepository;
    private PriceAlertLogRepository logRepository;
    private SecuritiesPriceService priceService;
    private PriceAlertService alertService;
    private UUID userId;

    @BeforeEach
    void setUp() {
        alertRepository = mock(PriceAlertRepository.class);
        logRepository = mock(PriceAlertLogRepository.class);
        priceService = mock(SecuritiesPriceService.class);
        alertService = new PriceAlertService(alertRepository, logRepository, priceService);
        userId = UUID.randomUUID();
    }


    @Test
    void testCreateAlert() {
        PriceAlert mockAlert = new PriceAlert(userId, "CRYPTO", "BTC", "BELOW", new BigDecimal("60000"), false);
        when(alertRepository.save(any(PriceAlert.class))).thenReturn(mockAlert);

        PriceAlert created = alertService.createAlert(userId, "CRYPTO", "BTC", "BELOW", new BigDecimal("60000"), false);

        assertNotNull(created);
        assertEquals("BTC", created.getSymbol());
        assertEquals("BELOW", created.getConditionType());
        assertEquals(new BigDecimal("60000"), created.getTargetPrice());
        verify(alertRepository, times(1)).save(any(PriceAlert.class));
    }

    @Test
    void testCreateAlertThrowsOnInvalidPrice() {
        assertThrows(IllegalArgumentException.class, () ->
            alertService.createAlert(userId, "CRYPTO", "BTC", "BELOW", new BigDecimal("-100"), false)
        );
    }

    @Test
    void testTriggerAboveAlertForStock() {
        // Stock price triggering using the mocked priceService
        PriceAlert stockAlert = new PriceAlert(userId, "STOCK", "TSLA", "ABOVE", new BigDecimal("250"), false);
        when(alertRepository.findByUserIdAndTriggeredFalseOrderByCreatedAtDesc(userId))
                .thenReturn(Collections.singletonList(stockAlert));
        when(priceService.getCurrentPrice("TSLA")).thenReturn(new BigDecimal("260"));

        List<PriceAlert> triggered = alertService.checkAndTriggerAlertsForUser(userId);

        assertEquals(1, triggered.size());
        assertTrue(triggered.get(0).isTriggered());
        verify(alertRepository, times(1)).save(stockAlert);
    }

    @Test
    void testNotTriggeredBelowCondition() {
        PriceAlert stockAlert = new PriceAlert(userId, "STOCK", "AAPL", "BELOW", new BigDecimal("150"), false);
        when(alertRepository.findByUserIdAndTriggeredFalseOrderByCreatedAtDesc(userId))
                .thenReturn(Collections.singletonList(stockAlert));
        when(priceService.getCurrentPrice("AAPL")).thenReturn(new BigDecimal("160"));

        List<PriceAlert> triggered = alertService.checkAndTriggerAlertsForUser(userId);

        assertTrue(triggered.isEmpty());
        assertFalse(stockAlert.isTriggered());
        verify(alertRepository, never()).save(any(PriceAlert.class));
    }

    @Test
    void testRecurringAlertStaysActive() {
        // A recurring alert fires but stays active — triggered remains false,
        // but lastTriggeredAt is persisted (so it won't fire again today).
        PriceAlert recurringAlert = new PriceAlert(userId, "STOCK", "NVDA", "ABOVE", new BigDecimal("500"), true);
        when(alertRepository.findByUserIdAndTriggeredFalseOrderByCreatedAtDesc(userId))
                .thenReturn(Collections.singletonList(recurringAlert));
        when(alertRepository.save(any(PriceAlert.class))).thenReturn(recurringAlert);
        when(priceService.getCurrentPrice("NVDA")).thenReturn(new BigDecimal("600"));

        List<PriceAlert> triggered = alertService.checkAndTriggerAlertsForUser(userId);

        assertEquals(1, triggered.size());
        // Recurring alert keeps triggered=false (stays in active list tomorrow)
        assertFalse(recurringAlert.isTriggered());
        // lastTriggeredAt must be set and persisted
        assertNotNull(recurringAlert.getLastTriggeredAt());
        verify(alertRepository, times(1)).save(recurringAlert);
    }
}
