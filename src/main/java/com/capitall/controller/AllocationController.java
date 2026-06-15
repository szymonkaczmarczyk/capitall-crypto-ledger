package com.capitall.controller;

import com.capitall.dto.AllocationDto;
import com.capitall.dto.CreateAllocationRequest;
import com.capitall.service.AllocationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/allocations")
public class AllocationController {

    private final AllocationService allocationService;

    public AllocationController(AllocationService allocationService) {
        this.allocationService = allocationService;
    }

    @PostMapping
    public ResponseEntity<AllocationDto> createAllocation(@Valid @RequestBody CreateAllocationRequest request) {
        AllocationDto dto = allocationService.createAllocation(request);
        return new ResponseEntity<>(dto, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AllocationDto> getAllocationById(@PathVariable UUID id) {
        AllocationDto dto = allocationService.getAllocationById(id);
        return ResponseEntity.ok(dto);
    }

    @GetMapping
    public ResponseEntity<List<AllocationDto>> getAllAllocations() {
        List<AllocationDto> allocations = allocationService.getAllAllocations();
        return ResponseEntity.ok(allocations);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AllocationDto>> getAllocationsByUser(@PathVariable UUID userId) {
        List<AllocationDto> allocations = allocationService.getAllocationsByUser(userId);
        return ResponseEntity.ok(allocations);
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<AllocationDto>> getAllocationsByAccount(@PathVariable UUID accountId) {
        List<AllocationDto> allocations = allocationService.getAllocationsByAccount(accountId);
        return ResponseEntity.ok(allocations);
    }

    @PostMapping("/{id}/expire")
    public ResponseEntity<Void> expireAllocation(@PathVariable UUID id) {
        allocationService.expireAllocation(id);
        return ResponseEntity.ok().build();
    }
}
