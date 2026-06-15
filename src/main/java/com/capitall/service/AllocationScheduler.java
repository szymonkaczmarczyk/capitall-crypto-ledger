package com.capitall.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AllocationScheduler {

    private final AllocationService allocationService;

    public AllocationScheduler(AllocationService allocationService) {
        this.allocationService = allocationService;
    }

    @Scheduled(fixedRateString = "${capitall.scheduler.clean-expired-ms:60000}")
    public void cleanExpiredAllocations() {
        log.info("Starting scheduled cleanup of expired allocations...");
        try {
            allocationService.cleanExpiredAllocations();
            log.info("Scheduled cleanup of expired allocations completed successfully.");
        } catch (Exception e) {
            log.error("Error occurred during scheduled cleanup of expired allocations", e);
        }
    }
}
