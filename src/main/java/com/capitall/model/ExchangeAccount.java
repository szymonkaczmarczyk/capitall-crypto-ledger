package com.capitall.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "exchange_accounts", indexes = {
    @Index(name = "idx_account_exchange", columnList = "exchangeName"),
    @Index(name = "idx_account_active", columnList = "isActive")
})
public class ExchangeAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "exchange_name", nullable = false, length = 50)
    private String exchangeName;

    @Column(name = "account_name", nullable = false, length = 100)
    private String accountName;

    @Column(name = "api_key", nullable = false, length = 512)
    private String encryptedApiKey;

    @Column(name = "api_secret", nullable = false, length = 512)
    private String encryptedApiSecret;

    @Column(name = "allocated_capital", nullable = false, precision = 19, scale = 4)
    private BigDecimal allocatedCapital;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public ExchangeAccount() {}

    public ExchangeAccount(UUID id, String exchangeName, String accountName, String encryptedApiKey, String encryptedApiSecret, BigDecimal allocatedCapital, boolean isActive, LocalDateTime createdAt) {
        this.id = id;
        this.exchangeName = exchangeName;
        this.accountName = accountName;
        this.encryptedApiKey = encryptedApiKey;
        this.encryptedApiSecret = encryptedApiSecret;
        this.allocatedCapital = allocatedCapital;
        this.isActive = isActive;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getExchangeName() { return exchangeName; }
    public void setExchangeName(String exchangeName) { this.exchangeName = exchangeName; }

    public String getAccountName() { return accountName; }
    public void setAccountName(String accountName) { this.accountName = accountName; }

    public String getEncryptedApiKey() { return encryptedApiKey; }
    public void setEncryptedApiKey(String encryptedApiKey) { this.encryptedApiKey = encryptedApiKey; }

    public String getEncryptedApiSecret() { return encryptedApiSecret; }
    public void setEncryptedApiSecret(String encryptedApiSecret) { this.encryptedApiSecret = encryptedApiSecret; }

    public BigDecimal getAllocatedCapital() { return allocatedCapital; }
    public void setAllocatedCapital(BigDecimal allocatedCapital) { this.allocatedCapital = allocatedCapital; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private String exchangeName;
        private String accountName;
        private String encryptedApiKey;
        private String encryptedApiSecret;
        private BigDecimal allocatedCapital;
        private boolean isActive;
        private LocalDateTime createdAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder exchangeName(String exchangeName) { this.exchangeName = exchangeName; return this; }
        public Builder accountName(String accountName) { this.accountName = accountName; return this; }
        public Builder encryptedApiKey(String encryptedApiKey) { this.encryptedApiKey = encryptedApiKey; return this; }
        public Builder encryptedApiSecret(String encryptedApiSecret) { this.encryptedApiSecret = encryptedApiSecret; return this; }
        public Builder allocatedCapital(BigDecimal allocatedCapital) { this.allocatedCapital = allocatedCapital; return this; }
        public Builder isActive(boolean isActive) { this.isActive = isActive; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public ExchangeAccount build() {
            ExchangeAccount account = new ExchangeAccount();
            account.id = this.id;
            account.exchangeName = this.exchangeName;
            account.accountName = this.accountName;
            account.encryptedApiKey = this.encryptedApiKey;
            account.encryptedApiSecret = this.encryptedApiSecret;
            account.allocatedCapital = this.allocatedCapital;
            account.isActive = this.isActive;
            account.createdAt = this.createdAt;
            return account;
        }
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (allocatedCapital == null) {
            allocatedCapital = BigDecimal.ZERO;
        }
    }
}
