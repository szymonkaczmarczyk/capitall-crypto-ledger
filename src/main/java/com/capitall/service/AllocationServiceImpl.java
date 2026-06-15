package com.capitall.service;

import com.capitall.dto.AllocationDto;
import com.capitall.dto.CreateAllocationRequest;
import com.capitall.exception.AssetAlreadyAllocatedException;
import com.capitall.exception.BusinessRuleException;
import com.capitall.exception.ResourceNotFoundException;
import com.capitall.model.*;
import com.capitall.repository.AllocationRepository;
import com.capitall.repository.ExchangeAccountRepository;
import com.capitall.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AllocationServiceImpl implements AllocationService {

    private final AllocationRepository allocationRepository;
    private final UserRepository userRepository;
    private final ExchangeAccountRepository exchangeAccountRepository;

    public AllocationServiceImpl(AllocationRepository allocationRepository,
                                  UserRepository userRepository,
                                  ExchangeAccountRepository exchangeAccountRepository) {
        this.allocationRepository = allocationRepository;
        this.userRepository = userRepository;
        this.exchangeAccountRepository = exchangeAccountRepository;
    }

    @Override
    @Transactional
    public AllocationDto createAllocation(CreateAllocationRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.userId()));

        ExchangeAccount account = exchangeAccountRepository.findById(request.exchangeAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Exchange account not found with id: " + request.exchangeAccountId()));

        if (!account.isActive()) {
            throw new BusinessRuleException("Cannot allocate an inactive exchange account: " + account.getAccountName());
        }

        if (account.getAllocatedCapital().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Cannot allocate an exchange account with zero or negative capital allocation: " + account.getAllocatedCapital());
        }

        List<Allocation> existingAllocations = allocationRepository.findByExchangeAccountId(account.getId());
        boolean isAlreadyAllocated = existingAllocations.stream()
                .anyMatch(alloc -> alloc.getStatus() == AllocationStatus.ACTIVE);

        if (isAlreadyAllocated) {
            throw new AssetAlreadyAllocatedException("Exchange account '" + account.getAccountName() + "' is already allocated to another active user");
        }

        Allocation allocation = Allocation.builder()
                .user(user)
                .exchangeAccount(account)
                .expiresAt(request.expiresAt())
                .status(AllocationStatus.ACTIVE)
                .build();

        Allocation savedAllocation = allocationRepository.save(allocation);
        return mapToDto(savedAllocation);
    }

    @Override
    public AllocationDto getAllocationById(UUID id) {
        return allocationRepository.findById(id)
                .map(this::mapToDto)
                .orElseThrow(() -> new ResourceNotFoundException("Allocation not found with id: " + id));
    }

    @Override
    public List<AllocationDto> getAllAllocations() {
        return allocationRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<AllocationDto> getAllocationsByUser(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
        return allocationRepository.findByUserId(userId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<AllocationDto> getAllocationsByAccount(UUID exchangeAccountId) {
        if (!exchangeAccountRepository.existsById(exchangeAccountId)) {
            throw new ResourceNotFoundException("Exchange account not found with id: " + exchangeAccountId);
        }
        return allocationRepository.findByExchangeAccountId(exchangeAccountId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void expireAllocation(UUID id) {
        Allocation allocation = allocationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Allocation not found with id: " + id));

        allocation.setStatus(AllocationStatus.EXPIRED);
        allocationRepository.save(allocation);
    }

    @Override
    @Transactional
    public void cleanExpiredAllocations() {
        List<Allocation> activeAllocations = allocationRepository.findByStatus(AllocationStatus.ACTIVE);
        LocalDateTime now = LocalDateTime.now();

        List<Allocation> expired = activeAllocations.stream()
                .filter(alloc -> alloc.getExpiresAt() != null && alloc.getExpiresAt().isBefore(now))
                .peek(alloc -> alloc.setStatus(AllocationStatus.EXPIRED))
                .collect(Collectors.toList());

        if (!expired.isEmpty()) {
            allocationRepository.saveAll(expired);
        }
    }

    private AllocationDto mapToDto(Allocation allocation) {
        return new AllocationDto(
                allocation.getId(),
                allocation.getUser().getId(),
                allocation.getUser().getUsername(),
                allocation.getUser().getRole().name(),
                allocation.getExchangeAccount().getId(),
                allocation.getExchangeAccount().getExchangeName(),
                allocation.getExchangeAccount().getAccountName(),
                allocation.getExchangeAccount().getAllocatedCapital(),
                allocation.getAssignedAt(),
                allocation.getExpiresAt(),
                allocation.getStatus()
        );
    }
}
