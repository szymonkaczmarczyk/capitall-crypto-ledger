package com.capitall.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "price_alert_logs")
public class PriceAlertLog {

    public enum Action { CREATED, EDITED, DELETED, TRIGGERED }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Action action;

    @Column(nullable = false)
    private String symbol;

    @Column(nullable = false)
    private String assetType;

    @Column(length = 512)
    private String details;

    @Column(nullable = false)
    private LocalDateTime occurredAt = LocalDateTime.now();

    public PriceAlertLog() {}

    public PriceAlertLog(UUID userId, Action action, String symbol, String assetType, String details) {
        this.userId = userId;
        this.action = action;
        this.symbol = symbol.toUpperCase();
        this.assetType = assetType.toUpperCase();
        this.details = details;
        this.occurredAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public Action getAction() { return action; }
    public String getSymbol() { return symbol; }
    public String getAssetType() { return assetType; }
    public String getDetails() { return details; }
    public LocalDateTime getOccurredAt() { return occurredAt; }

    public void setId(UUID id) { this.id = id; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public void setAction(Action action) { this.action = action; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public void setAssetType(String assetType) { this.assetType = assetType; }
    public void setDetails(String details) { this.details = details; }
    public void setOccurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; }
}
