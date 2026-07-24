package com.capitall.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "simulated_trades", indexes = {
        @Index(name = "idx_sim_trade_user", columnList = "user_id")
})
public class SimulatedTrade {

    public enum Side {
        BUY, SELL
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Side side;

    @Column(nullable = false, length = 16)
    private String symbol;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, precision = 30, scale = 8)
    private BigDecimal shares;

    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal price;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    public SimulatedTrade() {
    }

    public SimulatedTrade(UUID userId, Side side, String symbol, String name, BigDecimal shares, BigDecimal price) {
        this.userId = userId;
        this.side = side;
        this.symbol = symbol;
        this.name = name;
        this.shares = shares;
        this.price = price;
        this.timestamp = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public Side getSide() {
        return side;
    }

    public void setSide(Side side) {
        this.side = side;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getShares() {
        return shares;
    }

    public void setShares(BigDecimal shares) {
        this.shares = shares;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
