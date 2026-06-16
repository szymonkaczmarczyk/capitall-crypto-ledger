package com.capitall.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "holdings", uniqueConstraints = {
        @UniqueConstraint(name = "uk_holding_user_symbol", columnNames = {"user_id", "symbol"})
}, indexes = {
        @Index(name = "idx_holding_user", columnList = "user_id")
})
public class Holding {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 16)
    private String symbol;

    @Column(nullable = false, precision = 30, scale = 8)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "avg_cost", nullable = false, precision = 20, scale = 8)
    private BigDecimal avgCost = BigDecimal.ZERO;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Holding() {}

    public Holding(UUID userId, String symbol) {
        this.userId = userId;
        this.symbol = symbol;
    }

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getAvgCost() { return avgCost; }
    public void setAvgCost(BigDecimal avgCost) { this.avgCost = avgCost; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
