package com.capitall.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "target_allocations", uniqueConstraints = {
        @UniqueConstraint(name = "uk_target_user_symbol", columnNames = {"user_id", "symbol"})
})
public class TargetAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 16)
    private String symbol;

    @Column(name = "target_percentage", nullable = false, precision = 5, scale = 4)
    private BigDecimal targetPercentage = BigDecimal.ZERO; // e.g. 0.6000 for 60%

    public TargetAllocation() {}

    public TargetAllocation(UUID userId, String symbol, BigDecimal targetPercentage) {
        this.userId = userId;
        this.symbol = symbol;
        this.targetPercentage = targetPercentage;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public BigDecimal getTargetPercentage() { return targetPercentage; }
    public void setTargetPercentage(BigDecimal targetPercentage) { this.targetPercentage = targetPercentage; }
}
