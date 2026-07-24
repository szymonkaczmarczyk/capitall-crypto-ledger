package com.capitall.repository;

import com.capitall.model.SimulatedHolding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SimulatedHoldingRepository extends JpaRepository<SimulatedHolding, UUID> {
    List<SimulatedHolding> findByUserId(UUID userId);
    Optional<SimulatedHolding> findByUserIdAndSymbol(UUID userId, String symbol);
}
