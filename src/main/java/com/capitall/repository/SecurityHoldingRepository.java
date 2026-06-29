package com.capitall.repository;

import com.capitall.model.SecurityHolding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SecurityHoldingRepository extends JpaRepository<SecurityHolding, UUID> {
    List<SecurityHolding> findByUserId(UUID userId);
    Optional<SecurityHolding> findByUserIdAndSymbol(UUID userId, String symbol);
}
