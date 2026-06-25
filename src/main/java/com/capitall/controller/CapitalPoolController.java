package com.capitall.controller;

import com.capitall.dto.AdjustPoolBalanceRequest;
import com.capitall.dto.CapitalPoolDto;
import com.capitall.dto.CreatePoolRequest;
import com.capitall.dto.UpdatePoolStatusRequest;
import com.capitall.model.PoolStatus;
import com.capitall.service.CapitalPoolService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pools")
@PreAuthorize("hasRole('ADMIN')")
public class CapitalPoolController {

    private final CapitalPoolService capitalPoolService;

    public CapitalPoolController(CapitalPoolService capitalPoolService) {
        this.capitalPoolService = capitalPoolService;
    }

    @PostMapping
    public ResponseEntity<CapitalPoolDto> createPool(@Valid @RequestBody CreatePoolRequest request) {
        CapitalPoolDto created = capitalPoolService.createPool(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CapitalPoolDto> getPoolById(@PathVariable Long id) {
        CapitalPoolDto pool = capitalPoolService.getPoolById(id);
        return ResponseEntity.ok(pool);
    }

    @GetMapping
    public ResponseEntity<List<CapitalPoolDto>> getPools(
            @RequestParam(required = false) PoolStatus status,
            @RequestParam(required = false) Long managerId) {

        if (status != null) {
            return ResponseEntity.ok(capitalPoolService.getPoolsByStatus(status));
        } else if (managerId != null) {
            return ResponseEntity.ok(capitalPoolService.getPoolsByManager(managerId));
        } else {
            return ResponseEntity.ok(capitalPoolService.getAllPools());
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<CapitalPoolDto> updatePoolStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePoolStatusRequest request) {
        CapitalPoolDto updated = capitalPoolService.updatePoolStatus(id, request);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/balance")
    public ResponseEntity<CapitalPoolDto> adjustBalance(
            @PathVariable Long id,
            @Valid @RequestBody AdjustPoolBalanceRequest request) {
        CapitalPoolDto updated = capitalPoolService.adjustBalance(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePool(@PathVariable Long id) {
        capitalPoolService.deletePool(id);
        return ResponseEntity.noContent().build();
    }
}
