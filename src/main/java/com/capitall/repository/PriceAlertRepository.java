package com.capitall.repository;

import com.capitall.model.PriceAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PriceAlertRepository extends JpaRepository<PriceAlert, UUID> {
    List<PriceAlert> findByUserIdAndTriggeredFalseOrderByCreatedAtDesc(UUID userId);
    List<PriceAlert> findByTriggeredFalse();
}
