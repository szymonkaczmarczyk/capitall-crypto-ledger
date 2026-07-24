package com.capitall.service;

import com.capitall.dto.AllocationDto;
import com.capitall.dto.CreateAllocationRequest;
import com.capitall.exception.AssetAlreadyAllocatedException;
import com.capitall.exception.BusinessRuleException;
import com.capitall.model.*;
import com.capitall.repository.AllocationRepository;
import com.capitall.repository.ExchangeAccountRepository;
import com.capitall.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AllocationServiceImplTest {

    @Mock
    private AllocationRepository allocationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ExchangeAccountRepository exchangeAccountRepository;

    private AllocationService allocationService;

    @BeforeEach
    void setUp() {
        allocationService = new AllocationServiceImpl(allocationRepository, userRepository, exchangeAccountRepository);
    }

    @Test
    void createAllocation_ShouldSucceed_WhenValidRequest() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);

        User user = User.builder().id(userId).username("trader_bob").email("bob@quant.com").role(UserRole.USER).build();
        ExchangeAccount account = ExchangeAccount.builder()
                .id(accountId)
                .exchangeName("BINANCE")
                .accountName("Bob's Binance")
                .allocatedCapital(BigDecimal.valueOf(1000))
                .isActive(true)
                .build();

        CreateAllocationRequest request = new CreateAllocationRequest(userId, accountId, expiresAt);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(exchangeAccountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(allocationRepository.findByExchangeAccountId(accountId)).thenReturn(Collections.emptyList());

        Allocation savedAllocation = Allocation.builder()
                .id(UUID.randomUUID())
                .user(user)
                .exchangeAccount(account)
                .expiresAt(expiresAt)
                .status(AllocationStatus.ACTIVE)
                .build();

        when(allocationRepository.save(any(Allocation.class))).thenReturn(savedAllocation);

        AllocationDto result = allocationService.createAllocation(request);

        assertThat(result).isNotNull();
        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.exchangeAccountId()).isEqualTo(accountId);
        assertThat(result.status()).isEqualTo(AllocationStatus.ACTIVE);
        verify(allocationRepository).save(any(Allocation.class));
    }

    @Test
    void createAllocation_ShouldThrowException_WhenExchangeAccountIsInactive() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();

        User user = User.builder().id(userId).username("trader_bob").build();
        ExchangeAccount account = ExchangeAccount.builder()
                .id(accountId)
                .accountName("Bob's Binance")
                .allocatedCapital(BigDecimal.valueOf(1000))
                .isActive(false)
                .build();

        CreateAllocationRequest request = new CreateAllocationRequest(userId, accountId, null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(exchangeAccountRepository.findById(accountId)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> allocationService.createAllocation(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Cannot allocate an inactive exchange account");

        verify(allocationRepository, never()).save(any(Allocation.class));
    }

    @Test
    void createAllocation_ShouldThrowException_WhenCapitalIsZeroOrNegative() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();

        User user = User.builder().id(userId).username("trader_bob").build();
        ExchangeAccount account = ExchangeAccount.builder()
                .id(accountId)
                .accountName("Bob's Binance")
                .allocatedCapital(BigDecimal.ZERO)
                .isActive(true)
                .build();

        CreateAllocationRequest request = new CreateAllocationRequest(userId, accountId, null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(exchangeAccountRepository.findById(accountId)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> allocationService.createAllocation(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Cannot allocate an exchange account with zero or negative capital");

        verify(allocationRepository, never()).save(any(Allocation.class));
    }

    @Test
    void createAllocation_ShouldThrowException_WhenAlreadyAllocatedToActiveUser() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();

        User user = User.builder().id(userId).username("trader_bob").build();
        ExchangeAccount account = ExchangeAccount.builder()
                .id(accountId)
                .accountName("Bob's Binance")
                .allocatedCapital(BigDecimal.valueOf(500))
                .isActive(true)
                .build();

        CreateAllocationRequest request = new CreateAllocationRequest(userId, accountId, null);

        Allocation existingActiveAllocation = Allocation.builder()
                .id(UUID.randomUUID())
                .status(AllocationStatus.ACTIVE)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(exchangeAccountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(allocationRepository.findByExchangeAccountId(accountId)).thenReturn(List.of(existingActiveAllocation));

        assertThatThrownBy(() -> allocationService.createAllocation(request))
                .isInstanceOf(AssetAlreadyAllocatedException.class)
                .hasMessageContaining("is already allocated to another active user");

        verify(allocationRepository, never()).save(any(Allocation.class));
    }

    @Test
    void cleanExpiredAllocations_ShouldExpireAllocationsPastExpiration() {
        LocalDateTime past = LocalDateTime.now().minusMinutes(5);
        LocalDateTime future = LocalDateTime.now().plusDays(1);

        Allocation expiredAlloc = Allocation.builder()
                .id(UUID.randomUUID())
                .expiresAt(past)
                .status(AllocationStatus.ACTIVE)
                .build();

        Allocation activeAlloc = Allocation.builder()
                .id(UUID.randomUUID())
                .expiresAt(future)
                .status(AllocationStatus.ACTIVE)
                .build();

        when(allocationRepository.findByStatus(AllocationStatus.ACTIVE)).thenReturn(List.of(expiredAlloc, activeAlloc));

        allocationService.cleanExpiredAllocations();

        assertThat(expiredAlloc.getStatus()).isEqualTo(AllocationStatus.EXPIRED);
        assertThat(activeAlloc.getStatus()).isEqualTo(AllocationStatus.ACTIVE);
        verify(allocationRepository).saveAll(argThat(list -> {
            java.util.Collection<Allocation> col = (java.util.Collection<Allocation>) list;
            return col.size() == 1 && col.contains(expiredAlloc);
        }));
    }
}
