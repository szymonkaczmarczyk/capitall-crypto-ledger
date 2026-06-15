package com.capitall.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "api_keys", indexes = {
    @Index(name = "idx_key_pool", columnList = "pool_id"),
    @Index(name = "idx_key_trader", columnList = "trader_id"),
    @Index(name = "idx_key_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = KeyStatus.ACTIVE;
        }
    }
}
