package com.capitall.repository;

import com.capitall.model.EquitySnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface EquitySnapshotRepository extends JpaRepository<EquitySnapshot, UUID> {
    List<EquitySnapshot> findByUserIdAndCapturedAtGreaterThanEqualOrderByCapturedAtAsc(UUID userId, LocalDateTime from);
    List<EquitySnapshot> findByUserIdOrderByCapturedAtAsc(UUID userId);
}
