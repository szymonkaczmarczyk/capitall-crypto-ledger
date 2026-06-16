package com.capitall.repository;

import com.capitall.model.Holding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HoldingRepository extends JpaRepository<Holding, UUID> {
    Optional<Holding> findByUserIdAndSymbol(UUID userId, String symbol);
    List<Holding> findByUserId(UUID userId);
}
