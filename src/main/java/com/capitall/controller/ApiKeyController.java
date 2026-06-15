package com.capitall.controller;

import com.capitall.dto.ApiKeyCreatedResponse;
import com.capitall.dto.ApiKeyDto;
import com.capitall.dto.CreateApiKeyRequest;
import com.capitall.dto.SecretKeyRevealResponse;
import com.capitall.service.ApiKeyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/api-keys")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    public ApiKeyController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @PostMapping
    public ResponseEntity<ApiKeyCreatedResponse> createApiKey(@Valid @RequestBody CreateApiKeyRequest request) {
        ApiKeyCreatedResponse created = apiKeyService.createApiKey(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiKeyDto> getApiKeyById(@PathVariable Long id) {
        ApiKeyDto keyDto = apiKeyService.getApiKeyById(id);
        return ResponseEntity.ok(keyDto);
    }

    @GetMapping
    public ResponseEntity<List<ApiKeyDto>> getApiKeys(
            @RequestParam(required = false) Long poolId,
            @RequestParam(required = false) Long traderId) {

        if (poolId != null) {
            return ResponseEntity.ok(apiKeyService.getApiKeysByPool(poolId));
        } else if (traderId != null) {
            return ResponseEntity.ok(apiKeyService.getApiKeysByTrader(traderId));
        } else {
            return ResponseEntity.ok(apiKeyService.getAllApiKeys());
        }
    }

    @PostMapping("/{id}/revoke")
    public ResponseEntity<ApiKeyDto> revokeApiKey(@PathVariable Long id) {
        ApiKeyDto revoked = apiKeyService.revokeApiKey(id);
        return ResponseEntity.ok(revoked);
    }

    @GetMapping("/{id}/secret")
    public ResponseEntity<SecretKeyRevealResponse> revealSecretKey(@PathVariable Long id) {
        SecretKeyRevealResponse secret = apiKeyService.revealSecretKey(id);
        return ResponseEntity.ok(secret);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteApiKey(@PathVariable Long id) {
        apiKeyService.deleteApiKey(id);
        return ResponseEntity.noContent().build();
    }
}
