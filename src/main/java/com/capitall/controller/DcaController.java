package com.capitall.controller;

import com.capitall.model.RecurringOrder;
import com.capitall.model.User;
import com.capitall.service.DcaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/dca")
@PreAuthorize("isAuthenticated()")
public class DcaController {

    private final DcaService dcaService;

    public DcaController(DcaService dcaService) {
        this.dcaService = dcaService;
    }

    @GetMapping
    public ResponseEntity<List<RecurringOrder>> getUserOrders(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(dcaService.getUserOrders(user.getId()));
    }

    public record CreateDcaRequest(String symbol, BigDecimal usdAmount, Integer intervalDays, Boolean smartDca) {}

    @PostMapping
    public ResponseEntity<?> createOrder(
            @AuthenticationPrincipal User user,
            @RequestBody CreateDcaRequest request) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            RecurringOrder order = dcaService.createOrder(
                    user.getId(),
                    request.symbol(),
                    request.usdAmount(),
                    request.intervalDays(),
                    request.smartDca() != null ? request.smartDca() : false
            );
            return ResponseEntity.ok(order);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteOrder(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            dcaService.deleteOrder(user.getId(), id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/toggle")
    public ResponseEntity<?> toggleOrder(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            RecurringOrder order = dcaService.toggleOrder(user.getId(), id);
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
