package com.capitall.repository;

import com.capitall.model.Trader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TraderRepository extends JpaRepository<Trader, Long> {
    Optional<Trader> findByEmail(String email);
    boolean existsByEmail(String email);
}
