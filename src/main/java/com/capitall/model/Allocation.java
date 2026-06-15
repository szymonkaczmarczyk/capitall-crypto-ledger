package com.capitall.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "allocations", indexes = {
    @Index(name = "idx_allocation_user", columnList = "user_id"),
    @Index(name = "idx_allocation_account", columnList = "exchange_account_id"),
    @Index(name = "idx_allocation_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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

    @PrePersist
    protected void onCreate() {
        assignedAt = LocalDateTime.now();
        if (status == null) {
            status = AllocationStatus.ACTIVE;
        }
    }
}
