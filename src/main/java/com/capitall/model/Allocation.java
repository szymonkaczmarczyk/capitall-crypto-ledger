package com.capitall.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "allocations", indexes = {
    @Index(name = "idx_allocation_user", columnList = "user_id"),
    @Index(name = "idx_allocation_account", columnList = "exchange_account_id"),
    @Index(name = "idx_allocation_status", columnList = "status")
})
public class Allocation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exchange_account_id", nullable = false)
    private ExchangeAccount exchangeAccount;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private LocalDateTime assignedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AllocationStatus status;

    public Allocation() {}

    public Allocation(UUID id, User user, ExchangeAccount exchangeAccount, LocalDateTime assignedAt, LocalDateTime expiresAt, AllocationStatus status) {
        this.id = id;
        this.user = user;
        this.exchangeAccount = exchangeAccount;
        this.assignedAt = assignedAt;
        this.expiresAt = expiresAt;
        this.status = status;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public ExchangeAccount getExchangeAccount() { return exchangeAccount; }
    public void setExchangeAccount(ExchangeAccount exchangeAccount) { this.exchangeAccount = exchangeAccount; }

    public LocalDateTime getAssignedAt() { return assignedAt; }
    public void setAssignedAt(LocalDateTime assignedAt) { this.assignedAt = assignedAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public AllocationStatus getStatus() { return status; }
    public void setStatus(AllocationStatus status) { this.status = status; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private User user;
        private ExchangeAccount exchangeAccount;
        private LocalDateTime assignedAt;
        private LocalDateTime expiresAt;
        private AllocationStatus status;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder user(User user) { this.user = user; return this; }
        public Builder exchangeAccount(ExchangeAccount exchangeAccount) { this.exchangeAccount = exchangeAccount; return this; }
        public Builder assignedAt(LocalDateTime assignedAt) { this.assignedAt = assignedAt; return this; }
        public Builder expiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; return this; }
        public Builder status(AllocationStatus status) { this.status = status; return this; }

        public Allocation build() {
            Allocation allocation = new Allocation();
            allocation.id = this.id;
            allocation.user = this.user;
            allocation.exchangeAccount = this.exchangeAccount;
            allocation.assignedAt = this.assignedAt;
            allocation.expiresAt = this.expiresAt;
            allocation.status = this.status;
            return allocation;
        }
    }

    @PrePersist
    protected void onCreate() {
        assignedAt = LocalDateTime.now();
        if (status == null) {
            status = AllocationStatus.ACTIVE;
        }
    }
}
