package com.capitall.repository;

import com.capitall.model.Trade;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TradeRepository extends JpaRepository<Trade, UUID> {
    List<Trade> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
}
