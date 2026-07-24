package com.capitall.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "recurring_orders", indexes = {
        @Index(name = "idx_recurring_user", columnList = "user_id"),
        @Index(name = "idx_recurring_active_next", columnList = "active, next_execution")
})
public class RecurringOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 16)
    private String symbol;

    @Column(name = "usd_amount", nullable = false, precision = 20, scale = 2)
    private BigDecimal usdAmount;

    @Column(name = "interval_days", nullable = false)
    private Integer intervalDays = 7;

    @Column(name = "next_execution", nullable = false)
    private LocalDateTime nextExecution;

    @Column(name = "last_execution")
    private LocalDateTime lastExecution;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public RecurringOrder() {}

    public RecurringOrder(UUID userId, String symbol, BigDecimal usdAmount, Integer intervalDays, LocalDateTime nextExecution) {
        this.userId = userId;
        this.symbol = symbol;
        this.usdAmount = usdAmount;
        this.intervalDays = intervalDays;
        this.nextExecution = nextExecution;
        this.active = true;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public BigDecimal getUsdAmount() { return usdAmount; }
    public void setUsdAmount(BigDecimal usdAmount) { this.usdAmount = usdAmount; }

    public Integer getIntervalDays() { return intervalDays; }
    public void setIntervalDays(Integer intervalDays) { this.intervalDays = intervalDays; }

    public LocalDateTime getNextExecution() { return nextExecution; }
    public void setNextExecution(LocalDateTime nextExecution) { this.nextExecution = nextExecution; }

    public LocalDateTime getLastExecution() { return lastExecution; }
    public void setLastExecution(LocalDateTime lastExecution) { this.lastExecution = lastExecution; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
