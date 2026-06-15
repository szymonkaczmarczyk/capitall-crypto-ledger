package com.capitall.repository;

import com.capitall.model.ApiKey;
import com.capitall.model.KeyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {
    List<ApiKey> findByPoolId(Long poolId);
    List<ApiKey> findByTraderId(Long traderId);
    List<ApiKey> findByStatus(KeyStatus status);
}
