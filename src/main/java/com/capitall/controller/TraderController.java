package com.capitall.controller;

import com.capitall.dto.CreateTraderRequest;
import com.capitall.dto.TraderDto;
import com.capitall.service.TraderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/traders")
public class TraderController {

    private final TraderService traderService;

    public TraderController(TraderService traderService) {
        this.traderService = traderService;
    }

    @PostMapping
    public ResponseEntity<TraderDto> createTrader(@Valid @RequestBody CreateTraderRequest request) {
        TraderDto created = traderService.createTrader(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TraderDto> getTraderById(@PathVariable Long id) {
        TraderDto trader = traderService.getTraderById(id);
        return ResponseEntity.ok(trader);
    }

    @GetMapping
    public ResponseEntity<List<TraderDto>> getAllTraders() {
        List<TraderDto> traders = traderService.getAllTraders();
        return ResponseEntity.ok(traders);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTrader(@PathVariable Long id) {
        traderService.deleteTrader(id);
        return ResponseEntity.noContent().build();
    }
}
