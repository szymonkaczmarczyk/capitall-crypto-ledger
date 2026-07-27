package com.capitall.repository;

import com.capitall.model.TradingStrategy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TradingStrategyRepository extends JpaRepository<TradingStrategy, UUID> {
    List<TradingStrategy> findByUserIdOrderByCreatedAtDesc(UUID userId);
    List<TradingStrategy> findByActiveTrue();
}
