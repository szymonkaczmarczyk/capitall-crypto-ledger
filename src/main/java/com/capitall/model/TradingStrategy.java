package com.capitall.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "trading_strategies", indexes = {
    @Index(name = "idx_strat_user", columnList = "user_id"),
    @Index(name = "idx_strat_active", columnList = "active")
})
public class TradingStrategy {

    public enum TriggerType {
        DCA_SCHEDULED("DCA Czasowe (Co N godzin/dni)"),
        SMART_DIP("Smart Dip (Kupuj spadki o X%)"),
        RSI_OVERSOLD("RSI Wyprzedanie (RSI < 30)"),
        EMA_CROSSOVER("Przecięcie Średnich Kroczących");

        private final String displayName;

        TriggerType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Column(nullable = false, length = 50)
    private String exchange = "Capitall Simulator";

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, length = 30)
    private TriggerType triggerType = TriggerType.DCA_SCHEDULED;

    @Column(name = "interval_minutes")
    private Integer intervalMinutes = 1440; // 24 hours default

    @Column(name = "dip_percentage", precision = 5, scale = 2)
    private BigDecimal dipPercentage = new BigDecimal("5.00");

    @Column(name = "trade_amount_usd", nullable = false, precision = 19, scale = 4)
    private BigDecimal tradeAmountUsd = new BigDecimal("100.00");

    @Column(name = "take_profit_percent", precision = 5, scale = 2)
    private BigDecimal takeProfitPercent = new BigDecimal("10.00");

    @Column(name = "stop_loss_percent", precision = 5, scale = 2)
    private BigDecimal stopLossPercent = new BigDecimal("5.00");

    @Column(name = "trailing_stop_percent", precision = 5, scale = 2)
    private BigDecimal trailingStopPercent = new BigDecimal("3.00");

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "total_trades_count")
    private int totalTradesCount = 0;

    @Column(name = "total_profit_usd", precision = 19, scale = 4)
    private BigDecimal totalProfitUsd = BigDecimal.ZERO;

    @Column(name = "last_executed_at")
    private LocalDateTime lastExecutedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public TradingStrategy() {}

    public TradingStrategy(UUID userId, String name, String symbol, String exchange, TriggerType triggerType,
                           BigDecimal tradeAmountUsd, BigDecimal trailingStopPercent) {
        this.userId = userId;
        this.name = name;
        this.symbol = symbol;
        this.exchange = exchange;
        this.triggerType = triggerType;
        this.tradeAmountUsd = tradeAmountUsd;
        this.trailingStopPercent = trailingStopPercent;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getExchange() { return exchange; }
    public void setExchange(String exchange) { this.exchange = exchange; }

    public TriggerType getTriggerType() { return triggerType; }
    public void setTriggerType(TriggerType triggerType) { this.triggerType = triggerType; }

    public Integer getIntervalMinutes() { return intervalMinutes; }
    public void setIntervalMinutes(Integer intervalMinutes) { this.intervalMinutes = intervalMinutes; }

    public BigDecimal getDipPercentage() { return dipPercentage; }
    public void setDipPercentage(BigDecimal dipPercentage) { this.dipPercentage = dipPercentage; }

    public BigDecimal getTradeAmountUsd() { return tradeAmountUsd; }
    public void setTradeAmountUsd(BigDecimal tradeAmountUsd) { this.tradeAmountUsd = tradeAmountUsd; }

    public BigDecimal getTakeProfitPercent() { return takeProfitPercent; }
    public void setTakeProfitPercent(BigDecimal takeProfitPercent) { this.takeProfitPercent = takeProfitPercent; }

    public BigDecimal getStopLossPercent() { return stopLossPercent; }
    public void setStopLossPercent(BigDecimal stopLossPercent) { this.stopLossPercent = stopLossPercent; }

    public BigDecimal getTrailingStopPercent() { return trailingStopPercent; }
    public void setTrailingStopPercent(BigDecimal trailingStopPercent) { this.trailingStopPercent = trailingStopPercent; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public int getTotalTradesCount() { return totalTradesCount; }
    public void setTotalTradesCount(int totalTradesCount) { this.totalTradesCount = totalTradesCount; }

    public BigDecimal getTotalProfitUsd() { return totalProfitUsd; }
    public void setTotalProfitUsd(BigDecimal totalProfitUsd) { this.totalProfitUsd = totalProfitUsd; }

    public LocalDateTime getLastExecutedAt() { return lastExecutedAt; }
    public void setLastExecutedAt(LocalDateTime lastExecutedAt) { this.lastExecutedAt = lastExecutedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
