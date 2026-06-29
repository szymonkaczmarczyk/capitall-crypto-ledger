package com.capitall.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "security_trades", indexes = {
        @Index(name = "idx_security_trade_user", columnList = "user_id"),
        @Index(name = "idx_security_trade_user_created", columnList = "user_id, created_at")
})
public class SecurityTrade {

    public enum Side { BUY, SELL }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 16)
    private String symbol;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private Side side;

    @Column(nullable = false, precision = 20, scale = 4)
    private BigDecimal shares = BigDecimal.ZERO;

    @Column(nullable = false, precision = 20, scale = 4)
    private BigDecimal price = BigDecimal.ZERO;

    @Column(nullable = false, precision = 20, scale = 4)
    private BigDecimal fee = BigDecimal.ZERO;

    @Column(nullable = false, precision = 20, scale = 4)
    private BigDecimal gross = BigDecimal.ZERO;

    @Column(nullable = false, length = 8)
    private String currency;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public SecurityTrade() {}

    @PrePersist
    void touch() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Side getSide() { return side; }
    public void setSide(Side side) { this.side = side; }

    public BigDecimal getShares() { return shares; }
    public void setShares(BigDecimal shares) { this.shares = shares; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public BigDecimal getFee() { return fee; }
    public void setFee(BigDecimal fee) { this.fee = fee; }

    public BigDecimal getGross() { return gross; }
    public void setGross(BigDecimal gross) { this.gross = gross; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
