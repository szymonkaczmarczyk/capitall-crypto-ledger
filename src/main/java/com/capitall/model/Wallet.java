package com.capitall.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "wallets", indexes = {
        @Index(name = "idx_wallet_user", columnList = "user_id", unique = true)
})
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "usd_balance", nullable = false, precision = 20, scale = 2)
    private BigDecimal usdBalance = BigDecimal.ZERO;

    @Column(name = "pln_balance", nullable = false, precision = 20, scale = 2)
    private BigDecimal plnBalance = BigDecimal.ZERO;

    @Column(name = "eur_balance", nullable = false, precision = 20, scale = 2)
    private BigDecimal eurBalance = BigDecimal.ZERO;

    @Column(name = "gbp_balance", nullable = false, precision = 20, scale = 2)
    private BigDecimal gbpBalance = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Wallet() {
    }

    public Wallet(UUID userId, BigDecimal usdBalance) {
        this.userId = userId;
        this.usdBalance = usdBalance;
        this.plnBalance = BigDecimal.ZERO;
        this.eurBalance = BigDecimal.ZERO;
        this.gbpBalance = BigDecimal.ZERO;
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
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

    public BigDecimal getUsdBalance() {
        return usdBalance;
    }

    public void setUsdBalance(BigDecimal usdBalance) {
        this.usdBalance = usdBalance;
    }

    public BigDecimal getPlnBalance() {
        return plnBalance;
    }

    public void setPlnBalance(BigDecimal plnBalance) {
        this.plnBalance = plnBalance;
    }

    public BigDecimal getEurBalance() {
        return eurBalance;
    }

    public void setEurBalance(BigDecimal eurBalance) {
        this.eurBalance = eurBalance;
    }

    public BigDecimal getGbpBalance() {
        return gbpBalance;
    }

    public void setGbpBalance(BigDecimal gbpBalance) {
        this.gbpBalance = gbpBalance;
    }

    public BigDecimal getBalance(String currency) {
        if (currency == null) return BigDecimal.ZERO;
        return switch (currency.toUpperCase()) {
            case "USD" -> getUsdBalance();
            case "PLN" -> getPlnBalance();
            case "EUR" -> getEurBalance();
            case "GBP" -> getGbpBalance();
            default -> BigDecimal.ZERO;
        };
    }

    public void setBalance(String currency, BigDecimal amount) {
        if (currency == null) return;
        BigDecimal val = amount != null ? amount : BigDecimal.ZERO;
        switch (currency.toUpperCase()) {
            case "USD" -> setUsdBalance(val);
            case "PLN" -> setPlnBalance(val);
            case "EUR" -> setEurBalance(val);
            case "GBP" -> setGbpBalance(val);
            default -> {}
        }
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
