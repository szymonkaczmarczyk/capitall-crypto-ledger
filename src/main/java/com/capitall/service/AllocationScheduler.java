package com.capitall.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AllocationScheduler {
    private static final Logger log = LoggerFactory.getLogger(AllocationScheduler.class);

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
