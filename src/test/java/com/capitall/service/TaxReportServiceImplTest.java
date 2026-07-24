package com.capitall.service;

import com.capitall.dto.TaxReportDto;
import com.capitall.dto.TaxReportDto.TaxRow;
import com.capitall.model.SecurityTrade;
import com.capitall.model.Trade;
import com.capitall.repository.SecurityTradeRepository;
import com.capitall.repository.TradeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaxReportServiceImplTest {

    @Mock
    private TradeRepository tradeRepository;

    @Mock
    private SecurityTradeRepository securityTradeRepository;

    @Mock
    private ExchangeRateService exchangeRateService;

    private TaxReportService taxReportService;

    @BeforeEach
    void setUp() {
        taxReportService = new TaxReportServiceImpl(tradeRepository, securityTradeRepository, exchangeRateService);
    }

    @Test
    void generateReport_ShouldCalculateFIFOCorrectly() {
        UUID userId = UUID.randomUUID();
        LocalDateTime baseTime = LocalDateTime.of(2026, 5, 10, 10, 0);

        when(exchangeRateService.convert(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> {
                    BigDecimal amt = invocation.getArgument(0);
                    String from = invocation.getArgument(1);
                    String to = invocation.getArgument(2);
                    if (from.equalsIgnoreCase(to)) return amt;
                    if ("USD".equalsIgnoreCase(from) && "PLN".equalsIgnoreCase(to)) {
                        return amt == null ? BigDecimal.ZERO : amt.multiply(BigDecimal.valueOf(4.00));
                    }
                    return amt;
                });

        // Scenario:
        // BUY 10 BTC @ $1000 each (fee $10)
        // BUY 5 BTC @ $2000 each (fee $5)
        // SELL 12 BTC @ $1500 each (fee $12)
        // FIFO matching should consume:
        // - 10 BTC from first BUY (cost: 10 * 1000 = 10000, revenue: 10 * 1500 = 15000)
        // - 2 BTC from second BUY (cost: 2 * 2000 = 4000, revenue: 2 * 1500 = 3000)
        // Conversions to PLN use a USD fallback rate of 4.00

        Trade t1 = new Trade();
        t1.setSymbol("BTC");
        t1.setSide(Trade.Side.BUY);
        t1.setCoinAmount(BigDecimal.valueOf(10));
        t1.setPrice(BigDecimal.valueOf(1000));
        t1.setFee(BigDecimal.valueOf(10));
        t1.setCreatedAt(baseTime);

        Trade t2 = new Trade();
        t2.setSymbol("BTC");
        t2.setSide(Trade.Side.BUY);
        t2.setCoinAmount(BigDecimal.valueOf(5));
        t2.setPrice(BigDecimal.valueOf(2000));
        t2.setFee(BigDecimal.valueOf(5));
        t2.setCreatedAt(baseTime.plusHours(1));

        Trade t3 = new Trade();
        t3.setSymbol("BTC");
        t3.setSide(Trade.Side.SELL);
        t3.setCoinAmount(BigDecimal.valueOf(12));
        t3.setPrice(BigDecimal.valueOf(1500));
        t3.setFee(BigDecimal.valueOf(12));
        t3.setCreatedAt(baseTime.plusHours(2));

        when(tradeRepository.findByUserIdOrderByCreatedAtAsc(userId)).thenReturn(List.of(t1, t2, t3));
        when(securityTradeRepository.findByUserIdOrderByCreatedAtAsc(userId)).thenReturn(Collections.emptyList());

        TaxReportDto report = taxReportService.generateReport(userId, 2026, "FIFO");

        assertThat(report).isNotNull();
        // Values are in PLN (using fallback rate of 4.00 PLN/USD)
        // Net profit in USD was 3976 -> 3976 * 4 = 15904 PLN
        // Tax = 19% of 15904 = 3021.76 PLN
        
        assertThat(report.getRows()).hasSize(2);
        assertThat(report.getNetProfit()).isEqualByComparingTo(BigDecimal.valueOf(15904.0).setScale(2));
        assertThat(report.getEstimatedTax()).isEqualByComparingTo(BigDecimal.valueOf(3021.76));
    }
}
