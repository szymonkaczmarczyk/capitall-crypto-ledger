package com.capitall.service;

import com.capitall.dto.ApiKeyCreatedResponse;
import com.capitall.dto.ApiKeyDto;
import com.capitall.dto.CreateApiKeyRequest;
import com.capitall.dto.SecretKeyRevealResponse;
import java.util.List;

public interface ApiKeyService {
    ApiKeyCreatedResponse createApiKey(CreateApiKeyRequest request);
    ApiKeyDto getApiKeyById(Long id);
    List<ApiKeyDto> getAllApiKeys();
    List<ApiKeyDto> getApiKeysByPool(Long poolId);
    List<ApiKeyDto> getApiKeysByTrader(Long traderId);
    ApiKeyDto revokeApiKey(Long id);
    SecretKeyRevealResponse revealSecretKey(Long id);
    void deleteApiKey(Long id);
}
