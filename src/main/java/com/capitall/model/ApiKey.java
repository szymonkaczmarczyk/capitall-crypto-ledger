package com.capitall.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "api_keys", indexes = {
    @Index(name = "idx_key_pool", columnList = "pool_id"),
    @Index(name = "idx_key_trader", columnList = "trader_id"),
    @Index(name = "idx_key_status", columnList = "status")
})
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(name = "public_key", nullable = false, length = 255)
    private String publicKey;

    @Column(name = "encrypted_secret_key", nullable = false, length = 512)
    private String encryptedSecretKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Exchange exchange;

    @ElementCollection(targetClass = Permission.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "api_key_permissions", joinColumns = @JoinColumn(name = "api_key_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "permission", nullable = false, length = 30)
    private Set<Permission> permissions;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private KeyStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pool_id", nullable = false)
    private CapitalPool pool;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trader_id", nullable = false)
    private Trader trader;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    public ApiKey() {}

    public ApiKey(Long id, String title, String publicKey, String encryptedSecretKey, Exchange exchange, Set<Permission> permissions, KeyStatus status, CapitalPool pool, Trader trader, LocalDateTime createdAt, LocalDateTime expiresAt) {
        this.id = id;
        this.title = title;
        this.publicKey = publicKey;
        this.encryptedSecretKey = encryptedSecretKey;
        this.exchange = exchange;
        this.permissions = permissions;
        this.status = status;
        this.pool = pool;
        this.trader = trader;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getPublicKey() { return publicKey; }
    public void setPublicKey(String publicKey) { this.publicKey = publicKey; }

    public String getEncryptedSecretKey() { return encryptedSecretKey; }
    public void setEncryptedSecretKey(String encryptedSecretKey) { this.encryptedSecretKey = encryptedSecretKey; }

    public Exchange getExchange() { return exchange; }
    public void setExchange(Exchange exchange) { this.exchange = exchange; }

    public Set<Permission> getPermissions() { return permissions; }
    public void setPermissions(Set<Permission> permissions) { this.permissions = permissions; }

    public KeyStatus getStatus() { return status; }
    public void setStatus(KeyStatus status) { this.status = status; }

    public CapitalPool getPool() { return pool; }
    public void setPool(CapitalPool pool) { this.pool = pool; }

    public Trader getTrader() { return trader; }
    public void setTrader(Trader trader) { this.trader = trader; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String title;
        private String publicKey;
        private String encryptedSecretKey;
        private Exchange exchange;
        private Set<Permission> permissions;
        private KeyStatus status;
        private CapitalPool pool;
        private Trader trader;
        private LocalDateTime createdAt;
        private LocalDateTime expiresAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder title(String title) { this.title = title; return this; }
        public Builder publicKey(String publicKey) { this.publicKey = publicKey; return this; }
        public Builder encryptedSecretKey(String encryptedSecretKey) { this.encryptedSecretKey = encryptedSecretKey; return this; }
        public Builder exchange(Exchange exchange) { this.exchange = exchange; return this; }
        public Builder permissions(Set<Permission> permissions) { this.permissions = permissions; return this; }
        public Builder status(KeyStatus status) { this.status = status; return this; }
        public Builder pool(CapitalPool pool) { this.pool = pool; return this; }
        public Builder trader(Trader trader) { this.trader = trader; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder expiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; return this; }

        public ApiKey build() {
            ApiKey key = new ApiKey();
            key.id = this.id;
            key.title = this.title;
            key.publicKey = this.publicKey;
            key.encryptedSecretKey = this.encryptedSecretKey;
            key.exchange = this.exchange;
            key.permissions = this.permissions;
            key.status = this.status;
            key.pool = this.pool;
            key.trader = this.trader;
            key.createdAt = this.createdAt;
            key.expiresAt = this.expiresAt;
            return key;
        }
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = KeyStatus.ACTIVE;
        }
    }
}
