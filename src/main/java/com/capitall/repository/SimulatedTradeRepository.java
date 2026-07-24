package com.capitall.repository;

import com.capitall.model.SimulatedTrade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SimulatedTradeRepository extends JpaRepository<SimulatedTrade, UUID> {
    List<SimulatedTrade> findByUserIdOrderByTimestampDesc(UUID userId);
}
