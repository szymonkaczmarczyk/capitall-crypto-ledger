package com.capitall.controller;

import com.capitall.dto.CreateExchangeAccountRequest;
import com.capitall.dto.ExchangeAccountDto;
import com.capitall.dto.ExchangeAccountResponse;
import com.capitall.service.ExchangeAccountService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/assets")
@PreAuthorize("hasRole('ADMIN')")
public class AssetController {

    private final ExchangeAccountService exchangeAccountService;

    public AssetController(ExchangeAccountService exchangeAccountService) {
        this.exchangeAccountService = exchangeAccountService;
    }

    @PostMapping
    public ResponseEntity<ExchangeAccountResponse> createAsset(@Valid @RequestBody CreateExchangeAccountRequest request) {
        ExchangeAccountDto dto = exchangeAccountService.createExchangeAccount(request);
        return new ResponseEntity<>(mapToResponse(dto), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExchangeAccountResponse> getAssetById(@PathVariable UUID id) {
        ExchangeAccountDto dto = exchangeAccountService.getExchangeAccountById(id);
        return ResponseEntity.ok(mapToResponse(dto));
    }

    @GetMapping
    public ResponseEntity<List<ExchangeAccountResponse>> searchAssets(
            @RequestParam(required = false) String exchange,
            @RequestParam(required = false) BigDecimal minCapital,
            @RequestParam(required = false) Boolean active,
            Sort sort) {

        List<ExchangeAccountResponse> results = exchangeAccountService.searchExchangeAccounts(exchange, minCapital, active, sort);
        return ResponseEntity.ok(results);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ExchangeAccountResponse> toggleAssetStatus(
            @PathVariable UUID id,
            @RequestParam boolean active) {

        ExchangeAccountDto dto = exchangeAccountService.toggleAccountActiveStatus(id, active);
        return ResponseEntity.ok(mapToResponse(dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAsset(@PathVariable UUID id) {
        exchangeAccountService.deleteExchangeAccount(id);
        return ResponseEntity.noContent().build();
    }

    private ExchangeAccountResponse mapToResponse(ExchangeAccountDto dto) {
        return new ExchangeAccountResponse(
                dto.id(),
                dto.exchangeName(),
                dto.accountName(),
                dto.allocatedCapital(),
                dto.isActive(),
                dto.createdAt()
        );
    }
}
