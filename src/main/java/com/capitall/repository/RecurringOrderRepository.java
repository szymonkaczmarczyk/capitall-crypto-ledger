package com.capitall.repository;

import com.capitall.model.RecurringOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface RecurringOrderRepository extends JpaRepository<RecurringOrder, UUID> {
    List<RecurringOrder> findByUserIdOrderByCreatedAtDesc(UUID userId);
    List<RecurringOrder> findByActiveTrueAndNextExecutionLessThanEqual(LocalDateTime now);
}
