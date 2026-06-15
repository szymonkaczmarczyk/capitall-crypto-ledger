package com.capitall.repository;

import com.capitall.model.CapitalPool;
import com.capitall.model.PoolStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CapitalPoolRepository extends JpaRepository<CapitalPool, Long> {
    List<CapitalPool> findByStatus(PoolStatus status);
    List<CapitalPool> findByManagerId(Long managerId);
}
