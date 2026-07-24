package com.capitall.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "simulated_wallets", indexes = {
        @Index(name = "idx_sim_wallet_user", columnList = "user_id", unique = true)
})
public class SimulatedWallet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(nullable = false, precision = 20, scale = 2)
    private BigDecimal balance = new BigDecimal("10000.00");

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public SimulatedWallet() {
    }

    public SimulatedWallet(UUID userId, BigDecimal balance) {
        this.userId = userId;
        this.balance = balance;
    }

    @PrePersist
    @PreUpdate
    void touch() {
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

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
