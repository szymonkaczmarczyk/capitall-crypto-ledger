package com.capitall.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "trades", indexes = {
        @Index(name = "idx_trade_user", columnList = "user_id"),
        @Index(name = "idx_trade_user_created", columnList = "user_id, created_at")
})
public class Trade {

    public enum Side { BUY, SELL }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 16)
    private String symbol;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private Side side;

    @Column(name = "coin_amount", nullable = false, precision = 30, scale = 8)
    private BigDecimal coinAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal price = BigDecimal.ZERO;

    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal fee = BigDecimal.ZERO;

    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal gross = BigDecimal.ZERO;

    @Column(name = "realized_pnl", nullable = false, precision = 20, scale = 8)
    private BigDecimal realizedPnl = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Trade() {}

    @PrePersist
    void touch() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public Side getSide() { return side; }
    public void setSide(Side side) { this.side = side; }
    public BigDecimal getCoinAmount() { return coinAmount; }
    public void setCoinAmount(BigDecimal coinAmount) { this.coinAmount = coinAmount; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getFee() { return fee; }
    public void setFee(BigDecimal fee) { this.fee = fee; }
    public BigDecimal getGross() { return gross; }
    public void setGross(BigDecimal gross) { this.gross = gross; }
    public BigDecimal getRealizedPnl() { return realizedPnl; }
    public void setRealizedPnl(BigDecimal realizedPnl) { this.realizedPnl = realizedPnl; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
