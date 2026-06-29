package com.capitall.repository;

import com.capitall.model.SecurityTrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SecurityTradeRepository extends JpaRepository<SecurityTrade, UUID> {
    List<SecurityTrade> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
