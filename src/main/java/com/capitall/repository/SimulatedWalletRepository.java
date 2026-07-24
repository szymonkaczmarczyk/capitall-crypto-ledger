package com.capitall.repository;

import com.capitall.model.SimulatedWallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface SimulatedWalletRepository extends JpaRepository<SimulatedWallet, UUID> {
    Optional<SimulatedWallet> findByUserId(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from SimulatedWallet w where w.userId = :userId")
    Optional<SimulatedWallet> findByUserIdForUpdate(UUID userId);
}
