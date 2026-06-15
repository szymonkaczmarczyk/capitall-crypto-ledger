package com.capitall.repository;

import com.capitall.model.Allocation;
import com.capitall.model.AllocationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AllocationRepository extends JpaRepository<Allocation, UUID> {
    List<Allocation> findByUserId(UUID userId);
    List<Allocation> findByExchangeAccountId(UUID exchangeAccountId);
    List<Allocation> findByStatus(AllocationStatus status);
}
