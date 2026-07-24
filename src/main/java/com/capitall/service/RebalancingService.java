package com.capitall.service;

import com.capitall.dto.RebalanceResultDto;
import com.capitall.model.TargetAllocation;
import java.util.List;
import java.util.UUID;

public interface RebalancingService {
    List<TargetAllocation> getTargets(UUID userId);
    void saveTargets(UUID userId, List<TargetAllocation> targets);
    RebalanceResultDto calculateRebalancing(UUID userId);
}
