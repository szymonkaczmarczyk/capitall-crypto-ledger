package com.capitall.service;

import com.capitall.dto.AdjustPoolBalanceRequest;
import com.capitall.dto.CapitalPoolDto;
import com.capitall.dto.CreatePoolRequest;
import com.capitall.dto.UpdatePoolStatusRequest;
import com.capitall.model.PoolStatus;
import java.util.List;

public interface CapitalPoolService {
    CapitalPoolDto createPool(CreatePoolRequest request);
    CapitalPoolDto getPoolById(Long id);
    List<CapitalPoolDto> getAllPools();
    List<CapitalPoolDto> getPoolsByStatus(PoolStatus status);
    List<CapitalPoolDto> getPoolsByManager(Long managerId);
    CapitalPoolDto updatePoolStatus(Long id, UpdatePoolStatusRequest request);
    CapitalPoolDto adjustBalance(Long id, AdjustPoolBalanceRequest request);
    void deletePool(Long id);
}
