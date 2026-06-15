package com.capitall.repository;

import com.capitall.model.ExchangeAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface ExchangeAccountRepository extends JpaRepository<ExchangeAccount, UUID>, JpaSpecificationExecutor<ExchangeAccount> {
    List<ExchangeAccount> findByExchangeName(String exchangeName);
    List<ExchangeAccount> findByIsActive(boolean isActive);

    @Query("SELECT COALESCE(SUM(ea.allocatedCapital), 0) FROM ExchangeAccount ea WHERE ea.isActive = true")
    BigDecimal calculateTotalAum();

    @Query("SELECT ea.exchangeName, SUM(ea.allocatedCapital) FROM ExchangeAccount ea GROUP BY ea.exchangeName")
    List<Object[]> getExchangeCapitalGrouped();
}
