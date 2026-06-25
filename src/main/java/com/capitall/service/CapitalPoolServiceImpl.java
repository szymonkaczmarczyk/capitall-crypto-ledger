package com.capitall.service;

import com.capitall.dto.AdjustPoolBalanceRequest;
import com.capitall.dto.CapitalPoolDto;
import com.capitall.dto.CreatePoolRequest;
import com.capitall.dto.UpdatePoolStatusRequest;
import com.capitall.exception.BusinessRuleException;
import com.capitall.exception.ResourceNotFoundException;
import com.capitall.model.CapitalPool;
import com.capitall.model.PoolStatus;
import com.capitall.model.Trader;
import com.capitall.repository.CapitalPoolRepository;
import com.capitall.repository.TraderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class CapitalPoolServiceImpl implements CapitalPoolService {

    private final CapitalPoolRepository capitalPoolRepository;
    private final TraderRepository traderRepository;

    public CapitalPoolServiceImpl(CapitalPoolRepository capitalPoolRepository, TraderRepository traderRepository) {
        this.capitalPoolRepository = capitalPoolRepository;
        this.traderRepository = traderRepository;
    }

    @Override
    @Transactional
    public CapitalPoolDto createPool(CreatePoolRequest request) {
        Trader manager = traderRepository.findById(request.managerId())
                .orElseThrow(() -> new ResourceNotFoundException("Manager (Trader) not found with id: " + request.managerId()));

        CapitalPool pool = CapitalPool.builder()
                .name(request.name())
                .description(request.description())
                .balance(request.initialBalance())
                .currency(request.currency().toUpperCase())
                .status(PoolStatus.ACTIVE)
                .manager(manager)
                .build();

        CapitalPool savedPool = capitalPoolRepository.save(pool);
        return mapToDto(savedPool);
    }

    @Override
    public CapitalPoolDto getPoolById(Long id) {
        return capitalPoolRepository.findById(id)
                .map(this::mapToDto)
                .orElseThrow(() -> new ResourceNotFoundException("Capital pool not found with id: " + id));
    }

    @Override
    public List<CapitalPoolDto> getAllPools() {
        return capitalPoolRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<CapitalPoolDto> getPoolsByStatus(PoolStatus status) {
        return capitalPoolRepository.findByStatus(status).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<CapitalPoolDto> getPoolsByManager(Long managerId) {
        return capitalPoolRepository.findByManagerId(managerId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CapitalPoolDto updatePoolStatus(Long id, UpdatePoolStatusRequest request) {
        CapitalPool pool = capitalPoolRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Capital pool not found with id: " + id));

        pool.setStatus(request.status());
        CapitalPool updatedPool = capitalPoolRepository.save(pool);
        return mapToDto(updatedPool);
    }

    @Override
    @Transactional
    public CapitalPoolDto adjustBalance(Long id, AdjustPoolBalanceRequest request) {
        CapitalPool pool = capitalPoolRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Capital pool not found with id: " + id));

        if (pool.getStatus() != PoolStatus.ACTIVE) {
            throw new BusinessRuleException("Cannot adjust balance on a pool that is " + pool.getStatus());
        }

        BigDecimal currentBalance = pool.getBalance();
        BigDecimal amount = request.amount();

        if (amount == null || amount.signum() <= 0) {
            throw new BusinessRuleException("Amount must be a positive value");
        }

        switch (request.operation()) {
            case DEPOSIT -> pool.setBalance(currentBalance.add(amount));
            case WITHDRAW -> {
                if (currentBalance.compareTo(amount) < 0) {
                    throw new BusinessRuleException("Insufficient funds. Available: " + currentBalance + " " + pool.getCurrency() + ", Requested: " + amount);
                }
                pool.setBalance(currentBalance.subtract(amount));
            }
        }

        CapitalPool updatedPool = capitalPoolRepository.save(pool);
        return mapToDto(updatedPool);
    }

    @Override
    @Transactional
    public void deletePool(Long id) {
        CapitalPool pool = capitalPoolRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Capital pool not found with id: " + id));

        if (pool.getStatus() == PoolStatus.ACTIVE) {
            throw new BusinessRuleException("Cannot delete an ACTIVE capital pool. Please set status to CLOSED first.");
        }

        capitalPoolRepository.delete(pool);
    }

    private CapitalPoolDto mapToDto(CapitalPool pool) {
        return new CapitalPoolDto(
                pool.getId(),
                pool.getName(),
                pool.getDescription(),
                pool.getBalance(),
                pool.getCurrency(),
                pool.getStatus(),
                pool.getManager().getId(),
                pool.getManager().getName(),
                pool.getCreatedAt(),
                pool.getUpdatedAt()
        );
    }
}
