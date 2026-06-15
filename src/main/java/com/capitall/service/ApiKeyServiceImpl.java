package com.capitall.service;

import com.capitall.dto.ApiKeyCreatedResponse;
import com.capitall.dto.ApiKeyDto;
import com.capitall.dto.CreateApiKeyRequest;
import com.capitall.dto.SecretKeyRevealResponse;
import com.capitall.exception.BusinessRuleException;
import com.capitall.exception.ResourceNotFoundException;
import com.capitall.model.ApiKey;
import com.capitall.model.CapitalPool;
import com.capitall.model.KeyStatus;
import com.capitall.model.Trader;
import com.capitall.repository.ApiKeyRepository;
import com.capitall.repository.CapitalPoolRepository;
import com.capitall.repository.TraderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ApiKeyServiceImpl implements ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final CapitalPoolRepository capitalPoolRepository;
    private final TraderRepository traderRepository;
    private final ApiSecretsEncryptor apiSecretsEncryptor;

    public ApiKeyServiceImpl(ApiKeyRepository apiKeyRepository,
                              CapitalPoolRepository capitalPoolRepository,
                              TraderRepository traderRepository,
                              ApiSecretsEncryptor apiSecretsEncryptor) {
        this.apiKeyRepository = apiKeyRepository;
        this.capitalPoolRepository = capitalPoolRepository;
        this.traderRepository = traderRepository;
        this.apiSecretsEncryptor = apiSecretsEncryptor;
    }

    @Override
    @Transactional
    public ApiKeyCreatedResponse createApiKey(CreateApiKeyRequest request) {
        CapitalPool pool = capitalPoolRepository.findById(request.poolId())
                .orElseThrow(() -> new ResourceNotFoundException("Capital pool not found with id: " + request.poolId()));

        Trader trader = traderRepository.findById(request.traderId())
                .orElseThrow(() -> new ResourceNotFoundException("Trader not found with id: " + request.traderId()));

        String encryptedSecret = apiSecretsEncryptor.encrypt(request.secretKey());

        ApiKey apiKey = ApiKey.builder()
                .title(request.title())
                .publicKey(request.publicKey())
                .encryptedSecretKey(encryptedSecret)
                .exchange(request.exchange())
                .permissions(request.permissions())
                .status(KeyStatus.ACTIVE)
                .pool(pool)
                .trader(trader)
                .expiresAt(request.expiresAt())
                .build();

        ApiKey savedKey = apiKeyRepository.save(apiKey);

        return new ApiKeyCreatedResponse(
                savedKey.getId(),
                savedKey.getTitle(),
                savedKey.getPublicKey(),
                request.secretKey(), 
                savedKey.getExchange(),
                savedKey.getPermissions(),
                savedKey.getStatus(),
                savedKey.getPool().getId(),
                savedKey.getTrader().getId(),
                savedKey.getCreatedAt(),
                savedKey.getExpiresAt()
        );
    }

    @Override
    public ApiKeyDto getApiKeyById(Long id) {
        return apiKeyRepository.findById(id)
                .map(this::mapToDto)
                .orElseThrow(() -> new ResourceNotFoundException("API key not found with id: " + id));
    }

    @Override
    public List<ApiKeyDto> getAllApiKeys() {
        return apiKeyRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ApiKeyDto> getApiKeysByPool(Long poolId) {
        if (!capitalPoolRepository.existsById(poolId)) {
            throw new ResourceNotFoundException("Capital pool not found with id: " + poolId);
        }
        return apiKeyRepository.findByPoolId(poolId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ApiKeyDto> getApiKeysByTrader(Long traderId) {
        if (!traderRepository.existsById(traderId)) {
            throw new ResourceNotFoundException("Trader not found with id: " + traderId);
        }
        return apiKeyRepository.findByTraderId(traderId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ApiKeyDto revokeApiKey(Long id) {
        ApiKey apiKey = apiKeyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("API key not found with id: " + id));

        apiKey.setStatus(KeyStatus.REVOKED);
        ApiKey updatedKey = apiKeyRepository.save(apiKey);
        return mapToDto(updatedKey);
    }

    @Override
    public SecretKeyRevealResponse revealSecretKey(Long id) {
        ApiKey apiKey = apiKeyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("API key not found with id: " + id));

        String decryptedSecret = apiSecretsEncryptor.decrypt(apiKey.getEncryptedSecretKey());
        return new SecretKeyRevealResponse(decryptedSecret);
    }

    @Override
    @Transactional
    public void deleteApiKey(Long id) {
        ApiKey apiKey = apiKeyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("API key not found with id: " + id));

        if (apiKey.getStatus() == KeyStatus.ACTIVE) {
            throw new BusinessRuleException("Cannot delete an ACTIVE API key. Please revoke it first.");
        }

        apiKeyRepository.delete(apiKey);
    }

    private ApiKeyDto mapToDto(ApiKey apiKey) {
        return new ApiKeyDto(
                apiKey.getId(),
                apiKey.getTitle(),
                apiKey.getPublicKey(),
                apiKey.getExchange(),
                apiKey.getPermissions(),
                apiKey.getStatus(),
                apiKey.getPool().getId(),
                apiKey.getPool().getName(),
                apiKey.getTrader().getId(),
                apiKey.getTrader().getName(),
                apiKey.getCreatedAt(),
                apiKey.getExpiresAt()
        );
    }
}
