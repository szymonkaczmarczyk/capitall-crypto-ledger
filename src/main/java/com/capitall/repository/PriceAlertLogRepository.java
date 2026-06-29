package com.capitall.repository;

import com.capitall.model.PriceAlertLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PriceAlertLogRepository extends JpaRepository<PriceAlertLog, UUID> {
    List<PriceAlertLog> findByUserIdOrderByOccurredAtDesc(UUID userId);
}
