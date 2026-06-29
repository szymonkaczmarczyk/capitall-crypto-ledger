package com.capitall.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "security_holdings", uniqueConstraints = {
        @UniqueConstraint(name = "uk_security_holding_user_symbol", columnNames = {"user_id", "symbol"})
}, indexes = {
        @Index(name = "idx_security_holding_user", columnList = "user_id")
})
public class SecurityHolding {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 16)
    private String symbol;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, precision = 20, scale = 4)
    private BigDecimal shares = BigDecimal.ZERO;

    @Column(name = "avg_cost", nullable = false, precision = 20, scale = 4)
    private BigDecimal avgCost = BigDecimal.ZERO;

    @Column(nullable = false, length = 8)
    private String currency;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public SecurityHolding() {}

    public SecurityHolding(UUID userId, String symbol, String name, String currency) {
        this.userId = userId;
        this.symbol = symbol;
        this.name = name;
        this.currency = currency;
    }

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getShares() { return shares; }
    public void setShares(BigDecimal shares) { this.shares = shares; }

    public BigDecimal getAvgCost() { return avgCost; }
    public void setAvgCost(BigDecimal avgCost) { this.avgCost = avgCost; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
