package com.capitall.service;

import com.capitall.dto.ApiKeyCreatedResponse;
import com.capitall.dto.ApiKeyDto;
import com.capitall.dto.CreateApiKeyRequest;
import com.capitall.dto.SecretKeyRevealResponse;
import com.capitall.exception.BusinessRuleException;
import com.capitall.model.*;
import com.capitall.repository.ApiKeyRepository;
import com.capitall.repository.CapitalPoolRepository;
import com.capitall.repository.TraderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiKeyServiceImplTest {

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @Mock
    private CapitalPoolRepository capitalPoolRepository;

    @Mock
    private TraderRepository traderRepository;

    @Mock
    private ApiSecretsEncryptor apiSecretsEncryptor;

    private ApiKeyService apiKeyService;

    @BeforeEach
    void setUp() {
        apiKeyService = new ApiKeyServiceImpl(apiKeyRepository, capitalPoolRepository, traderRepository, apiSecretsEncryptor);
    }

    @Test
    void createApiKey_ShouldEncryptSecretAndSave() {
        Long poolId = 10L;
        Long traderId = 1L;
        Trader trader = Trader.builder().id(traderId).name("Trader").email("trader@quant.com").build();
        CapitalPool pool = CapitalPool.builder().id(poolId).name("Pool").balance(BigDecimal.ZERO).currency("USD").manager(trader).build();

        CreateApiKeyRequest request = new CreateApiKeyRequest(
                "Binance Key", "pub_123", "secret_abc", Exchange.BINANCE,
                Set.of(Permission.READ, Permission.TRADE), poolId, traderId, LocalDateTime.now().plusDays(30)
        );

        when(capitalPoolRepository.findById(poolId)).thenReturn(Optional.of(pool));
        when(traderRepository.findById(traderId)).thenReturn(Optional.of(trader));
        when(apiSecretsEncryptor.encrypt("secret_abc")).thenReturn("encrypted_secret_abc");

        ApiKey savedKey = ApiKey.builder()
                .id(100L)
                .title("Binance Key")
                .publicKey("pub_123")
                .encryptedSecretKey("encrypted_secret_abc")
                .exchange(Exchange.BINANCE)
                .permissions(Set.of(Permission.READ, Permission.TRADE))
                .status(KeyStatus.ACTIVE)
                .pool(pool)
                .trader(trader)
                .expiresAt(request.expiresAt())
                .build();

        when(apiKeyRepository.save(any(ApiKey.class))).thenReturn(savedKey);

        ApiKeyCreatedResponse result = apiKeyService.createApiKey(request);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(100L);
        assertThat(result.secretKey()).isEqualTo("secret_abc");
        verify(apiSecretsEncryptor).encrypt("secret_abc");
        verify(apiKeyRepository).save(any(ApiKey.class));
    }

    @Test
    void revealSecretKey_ShouldDecryptAndReturnSecret() {
        Long keyId = 100L;
        Trader trader = Trader.builder().id(1L).name("Trader").email("trader@quant.com").build();
        CapitalPool pool = CapitalPool.builder().id(10L).name("Pool").balance(BigDecimal.ZERO).currency("USD").manager(trader).build();

        ApiKey apiKey = ApiKey.builder()
                .id(keyId)
                .title("Binance Key")
                .publicKey("pub_123")
                .encryptedSecretKey("encrypted_secret_abc")
                .exchange(Exchange.BINANCE)
                .status(KeyStatus.ACTIVE)
                .pool(pool)
                .trader(trader)
                .build();

        when(apiKeyRepository.findById(keyId)).thenReturn(Optional.of(apiKey));
        when(apiSecretsEncryptor.decrypt("encrypted_secret_abc")).thenReturn("decrypted_secret_abc");

        SecretKeyRevealResponse result = apiKeyService.revealSecretKey(keyId);

        assertThat(result).isNotNull();
        assertThat(result.secretKey()).isEqualTo("decrypted_secret_abc");
        verify(apiSecretsEncryptor).decrypt("encrypted_secret_abc");
    }

    @Test
    void revokeApiKey_ShouldSetStatusToRevoked() {
        Long keyId = 100L;
        Trader trader = Trader.builder().id(1L).name("Trader").email("trader@quant.com").build();
        CapitalPool pool = CapitalPool.builder().id(10L).name("Pool").balance(BigDecimal.ZERO).currency("USD").manager(trader).build();

        ApiKey apiKey = ApiKey.builder()
                .id(keyId)
                .title("Binance Key")
                .publicKey("pub_123")
                .encryptedSecretKey("encrypted_secret_abc")
                .exchange(Exchange.BINANCE)
                .status(KeyStatus.ACTIVE)
                .pool(pool)
                .trader(trader)
                .build();

        when(apiKeyRepository.findById(keyId)).thenReturn(Optional.of(apiKey));
        when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ApiKeyDto result = apiKeyService.revokeApiKey(keyId);

        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(KeyStatus.REVOKED);
        verify(apiKeyRepository).save(apiKey);
    }

    @Test
    void deleteApiKey_ShouldThrowException_WhenKeyIsActive() {
        Long keyId = 100L;
        Trader trader = Trader.builder().id(1L).name("Trader").email("trader@quant.com").build();
        CapitalPool pool = CapitalPool.builder().id(10L).name("Pool").balance(BigDecimal.ZERO).currency("USD").manager(trader).build();

        ApiKey apiKey = ApiKey.builder()
                .id(keyId)
                .status(KeyStatus.ACTIVE)
                .pool(pool)
                .trader(trader)
                .build();

        when(apiKeyRepository.findById(keyId)).thenReturn(Optional.of(apiKey));

        assertThatThrownBy(() -> apiKeyService.deleteApiKey(keyId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Cannot delete an ACTIVE API key");

        verify(apiKeyRepository, never()).delete(any(ApiKey.class));
    }

    @Test
    void deleteApiKey_ShouldDelete_WhenKeyIsRevoked() {
        Long keyId = 100L;
        Trader trader = Trader.builder().id(1L).name("Trader").email("trader@quant.com").build();
        CapitalPool pool = CapitalPool.builder().id(10L).name("Pool").balance(BigDecimal.ZERO).currency("USD").manager(trader).build();

        ApiKey apiKey = ApiKey.builder()
                .id(keyId)
                .status(KeyStatus.REVOKED)
                .pool(pool)
                .trader(trader)
                .build();

        when(apiKeyRepository.findById(keyId)).thenReturn(Optional.of(apiKey));

        apiKeyService.deleteApiKey(keyId);

        verify(apiKeyRepository).delete(apiKey);
    }
}
