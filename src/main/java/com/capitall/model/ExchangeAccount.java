package com.capitall.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "exchange_accounts", indexes = {
    @Index(name = "idx_account_exchange", columnList = "exchangeName"),
    @Index(name = "idx_account_active", columnList = "isActive")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (allocatedCapital == null) {
            allocatedCapital = BigDecimal.ZERO;
        }
    }
}
