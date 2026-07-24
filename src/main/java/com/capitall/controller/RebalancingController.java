package com.capitall.controller;

import com.capitall.dto.RebalanceResultDto;
import com.capitall.model.TargetAllocation;
import com.capitall.model.User;
import com.capitall.service.RebalancingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/rebalance")
@PreAuthorize("isAuthenticated()")
public class RebalancingController {

    private final RebalancingService rebalancingService;

    public RebalancingController(RebalancingService rebalancingService) {
        this.rebalancingService = rebalancingService;
    }

    @GetMapping
    public ResponseEntity<RebalanceResultDto> getRebalanceResult(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(rebalancingService.calculateRebalancing(user.getId()));
    }

    public record SaveTargetsRequest(String symbol, BigDecimal targetPercentage) {}

    @PostMapping("/targets")
    public ResponseEntity<?> saveTargets(
            @AuthenticationPrincipal User user,
            @RequestBody List<SaveTargetsRequest> requestList) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            List<TargetAllocation> targets = requestList.stream()
                    .map(req -> {
                        // Divide percentage by 100 to save as decimal (e.g. 60 -> 0.60)
                        BigDecimal pct = req.targetPercentage().divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP);
                        return new TargetAllocation(user.getId(), req.symbol().toUpperCase(), pct);
                    })
                    .collect(Collectors.toList());

            rebalancingService.saveTargets(user.getId(), targets);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
