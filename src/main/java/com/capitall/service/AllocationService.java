package com.capitall.service;

import com.capitall.dto.AllocationDto;
import com.capitall.dto.CreateAllocationRequest;
import java.util.List;
import java.util.UUID;

public interface AllocationService {
    AllocationDto createAllocation(CreateAllocationRequest request);
    AllocationDto getAllocationById(UUID id);
    List<AllocationDto> getAllAllocations();
    List<AllocationDto> getAllocationsByUser(UUID userId);
    List<AllocationDto> getAllocationsByAccount(UUID exchangeAccountId);
    void expireAllocation(UUID id);
    void cleanExpiredAllocations();
}
