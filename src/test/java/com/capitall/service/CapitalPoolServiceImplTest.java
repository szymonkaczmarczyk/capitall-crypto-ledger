package com.capitall.service;

import com.capitall.dto.AdjustPoolBalanceRequest;
import com.capitall.dto.BalanceOperation;
import com.capitall.dto.CapitalPoolDto;
import com.capitall.dto.CreatePoolRequest;
import com.capitall.exception.BusinessRuleException;
import com.capitall.exception.ResourceNotFoundException;
import com.capitall.model.CapitalPool;
import com.capitall.model.PoolStatus;
import com.capitall.model.Trader;
import com.capitall.repository.CapitalPoolRepository;
import com.capitall.repository.TraderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CapitalPoolServiceImplTest {

    @Mock
    private CapitalPoolRepository capitalPoolRepository;

    @Mock
    private TraderRepository traderRepository;

    private CapitalPoolService capitalPoolService;

    @BeforeEach
    void setUp() {
        capitalPoolService = new CapitalPoolServiceImpl(capitalPoolRepository, traderRepository);
    }

    @Test
    void createPool_ShouldSaveAndReturnPool_WhenManagerExists() {
        Long managerId = 1L;
        Trader manager = Trader.builder().id(managerId).name("Manager").email("mgr@quant.com").build();
        CreatePoolRequest request = new CreatePoolRequest("Alpha Pool", "Strategy A", BigDecimal.valueOf(10000), "USD", managerId);

        CapitalPool savedPool = CapitalPool.builder()
                .id(10L)
                .name("Alpha Pool")
                .description("Strategy A")
                .balance(BigDecimal.valueOf(10000))
                .currency("USD")
                .status(PoolStatus.ACTIVE)
                .manager(manager)
                .build();

        when(traderRepository.findById(managerId)).thenReturn(Optional.of(manager));
        when(capitalPoolRepository.save(any(CapitalPool.class))).thenReturn(savedPool);

        CapitalPoolDto result = capitalPoolService.createPool(request);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.balance()).isEqualByComparingTo(BigDecimal.valueOf(10000));
        assertThat(result.managerId()).isEqualTo(managerId);
        verify(traderRepository).findById(managerId);
        verify(capitalPoolRepository).save(any(CapitalPool.class));
    }

    @Test
    void createPool_ShouldThrowException_WhenManagerDoesNotExist() {
        Long managerId = 1L;
        CreatePoolRequest request = new CreatePoolRequest("Alpha Pool", "Strategy A", BigDecimal.valueOf(10000), "USD", managerId);
        when(traderRepository.findById(managerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> capitalPoolService.createPool(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Manager (Trader) not found");

        verify(traderRepository).findById(managerId);
        verify(capitalPoolRepository, never()).save(any(CapitalPool.class));
    }

    @Test
    void adjustBalance_ShouldDepositSuccessfully() {
        Long poolId = 10L;
        Trader manager = Trader.builder().id(1L).name("Manager").email("mgr@quant.com").build();
        CapitalPool pool = CapitalPool.builder()
                .id(poolId)
                .name("Alpha Pool")
                .balance(BigDecimal.valueOf(10000))
                .currency("USD")
                .status(PoolStatus.ACTIVE)
                .manager(manager)
                .build();

        AdjustPoolBalanceRequest request = new AdjustPoolBalanceRequest(BigDecimal.valueOf(5000), BalanceOperation.DEPOSIT);

        when(capitalPoolRepository.findById(poolId)).thenReturn(Optional.of(pool));
        when(capitalPoolRepository.save(any(CapitalPool.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CapitalPoolDto result = capitalPoolService.adjustBalance(poolId, request);

        assertThat(result).isNotNull();
        assertThat(result.balance()).isEqualByComparingTo(BigDecimal.valueOf(15000));
        verify(capitalPoolRepository).findById(poolId);
        verify(capitalPoolRepository).save(pool);
    }

    @Test
    void adjustBalance_ShouldWithdrawSuccessfully_WhenFundsAvailable() {
        Long poolId = 10L;
        Trader manager = Trader.builder().id(1L).name("Manager").email("mgr@quant.com").build();
        CapitalPool pool = CapitalPool.builder()
                .id(poolId)
                .name("Alpha Pool")
                .balance(BigDecimal.valueOf(10000))
                .currency("USD")
                .status(PoolStatus.ACTIVE)
                .manager(manager)
                .build();

        AdjustPoolBalanceRequest request = new AdjustPoolBalanceRequest(BigDecimal.valueOf(3000), BalanceOperation.WITHDRAW);

        when(capitalPoolRepository.findById(poolId)).thenReturn(Optional.of(pool));
        when(capitalPoolRepository.save(any(CapitalPool.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CapitalPoolDto result = capitalPoolService.adjustBalance(poolId, request);

        assertThat(result).isNotNull();
        assertThat(result.balance()).isEqualByComparingTo(BigDecimal.valueOf(7000));
        verify(capitalPoolRepository).findById(poolId);
        verify(capitalPoolRepository).save(pool);
    }

    @Test
    void adjustBalance_ShouldThrowException_WhenWithdrawAmountExceedsBalance() {
        Long poolId = 10L;
        Trader manager = Trader.builder().id(1L).name("Manager").email("mgr@quant.com").build();
        CapitalPool pool = CapitalPool.builder()
                .id(poolId)
                .name("Alpha Pool")
                .balance(BigDecimal.valueOf(10000))
                .currency("USD")
                .status(PoolStatus.ACTIVE)
                .manager(manager)
                .build();

        AdjustPoolBalanceRequest request = new AdjustPoolBalanceRequest(BigDecimal.valueOf(12000), BalanceOperation.WITHDRAW);

        when(capitalPoolRepository.findById(poolId)).thenReturn(Optional.of(pool));

        assertThatThrownBy(() -> capitalPoolService.adjustBalance(poolId, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Insufficient funds");

        verify(capitalPoolRepository).findById(poolId);
        verify(capitalPoolRepository, never()).save(any(CapitalPool.class));
    }

    @Test
    void adjustBalance_ShouldThrowException_WhenPoolIsNotActive() {
        Long poolId = 10L;
        Trader manager = Trader.builder().id(1L).name("Manager").email("mgr@quant.com").build();
        CapitalPool pool = CapitalPool.builder()
                .id(poolId)
                .name("Alpha Pool")
                .balance(BigDecimal.valueOf(10000))
                .currency("USD")
                .status(PoolStatus.PAUSED)
                .manager(manager)
                .build();

        AdjustPoolBalanceRequest request = new AdjustPoolBalanceRequest(BigDecimal.valueOf(1000), BalanceOperation.DEPOSIT);

        when(capitalPoolRepository.findById(poolId)).thenReturn(Optional.of(pool));

        assertThatThrownBy(() -> capitalPoolService.adjustBalance(poolId, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Cannot adjust balance on a pool that is PAUSED");

        verify(capitalPoolRepository).findById(poolId);
        verify(capitalPoolRepository, never()).save(any(CapitalPool.class));
    }

    @Test
    void deletePool_ShouldThrowException_WhenPoolIsActive() {
        Long poolId = 10L;
        Trader manager = Trader.builder().id(1L).name("Manager").email("mgr@quant.com").build();
        CapitalPool pool = CapitalPool.builder()
                .id(poolId)
                .name("Alpha Pool")
                .status(PoolStatus.ACTIVE)
                .manager(manager)
                .build();

        when(capitalPoolRepository.findById(poolId)).thenReturn(Optional.of(pool));

        assertThatThrownBy(() -> capitalPoolService.deletePool(poolId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Cannot delete an ACTIVE capital pool");

        verify(capitalPoolRepository, never()).delete(any(CapitalPool.class));
    }

    @Test
    void deletePool_ShouldDelete_WhenPoolIsClosed() {
        Long poolId = 10L;
        Trader manager = Trader.builder().id(1L).name("Manager").email("mgr@quant.com").build();
        CapitalPool pool = CapitalPool.builder()
                .id(poolId)
                .name("Alpha Pool")
                .status(PoolStatus.CLOSED)
                .manager(manager)
                .build();

        when(capitalPoolRepository.findById(poolId)).thenReturn(Optional.of(pool));

        capitalPoolService.deletePool(poolId);

        verify(capitalPoolRepository).delete(pool);
    }
}
