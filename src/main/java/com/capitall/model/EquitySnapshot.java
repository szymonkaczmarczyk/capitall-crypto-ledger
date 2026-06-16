package com.capitall.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "equity_snapshots", indexes = {
        @Index(name = "idx_equity_user_ts", columnList = "user_id, captured_at")
})
public class EquitySnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "equity_usd", nullable = false, precision = 20, scale = 2)
    private BigDecimal equityUsd = BigDecimal.ZERO;

    @Column(name = "cash_usd", nullable = false, precision = 20, scale = 2)
    private BigDecimal cashUsd = BigDecimal.ZERO;

    @Column(name = "crypto_usd", nullable = false, precision = 20, scale = 2)
    private BigDecimal cryptoUsd = BigDecimal.ZERO;

    @Column(name = "captured_at", nullable = false)
    private LocalDateTime capturedAt;

    public EquitySnapshot() {}

    @PrePersist
    void touch() {
        if (capturedAt == null) capturedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public BigDecimal getEquityUsd() { return equityUsd; }
    public void setEquityUsd(BigDecimal equityUsd) { this.equityUsd = equityUsd; }
    public BigDecimal getCashUsd() { return cashUsd; }
    public void setCashUsd(BigDecimal cashUsd) { this.cashUsd = cashUsd; }
    public BigDecimal getCryptoUsd() { return cryptoUsd; }
    public void setCryptoUsd(BigDecimal cryptoUsd) { this.cryptoUsd = cryptoUsd; }
    public LocalDateTime getCapturedAt() { return capturedAt; }
    public void setCapturedAt(LocalDateTime capturedAt) { this.capturedAt = capturedAt; }
}
