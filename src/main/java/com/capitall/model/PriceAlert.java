package com.capitall.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "price_alerts")
public class PriceAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String assetType;

    @Column(nullable = false)
    private String symbol;

    @Column(nullable = false)
    private String conditionType;

    @Column(nullable = false, precision = 18, scale = 8)
    private BigDecimal targetPrice;

    @Column(nullable = false)
    private boolean triggered = false;

    @Column(nullable = false)
    private boolean recurring = false;

    @Column(nullable = true)
    private LocalDateTime lastTriggeredAt;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public PriceAlert() {
    }

    public PriceAlert(UUID userId, String assetType, String symbol, String conditionType, BigDecimal targetPrice, boolean recurring) {
        this.userId = userId;
        this.assetType = assetType.toUpperCase();
        this.symbol = symbol.toUpperCase();
        this.conditionType = conditionType.toUpperCase();
        this.targetPrice = targetPrice;
        this.recurring = recurring;
        this.triggered = false;
        this.createdAt = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getAssetType() {
        return assetType;
    }

    public void setAssetType(String assetType) {
        this.assetType = assetType;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getConditionType() {
        return conditionType;
    }

    public void setConditionType(String conditionType) {
        this.conditionType = conditionType;
    }

    public BigDecimal getTargetPrice() {
        return targetPrice;
    }

    public void setTargetPrice(BigDecimal targetPrice) {
        this.targetPrice = targetPrice;
    }

    public boolean isTriggered() {
        return triggered;
    }

    public void setTriggered(boolean triggered) {
        this.triggered = triggered;
    }

    public boolean isRecurring() {
        return recurring;
    }

    public void setRecurring(boolean recurring) {
        this.recurring = recurring;
    }

    public LocalDateTime getLastTriggeredAt() {
        return lastTriggeredAt;
    }

    public void setLastTriggeredAt(LocalDateTime lastTriggeredAt) {
        this.lastTriggeredAt = lastTriggeredAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
