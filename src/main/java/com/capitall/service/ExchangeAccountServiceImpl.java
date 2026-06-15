package com.capitall.service;

import com.capitall.dto.CreateExchangeAccountRequest;
import com.capitall.dto.ExchangeAccountDto;
import com.capitall.dto.ExchangeAccountResponse;
import com.capitall.exception.ResourceNotFoundException;
import com.capitall.model.ExchangeAccount;
import com.capitall.repository.ExchangeAccountRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ExchangeAccountServiceImpl implements ExchangeAccountService {

    private final ExchangeAccountRepository exchangeAccountRepository;
    private final ApiSecretsEncryptor apiSecretsEncryptor;

    public ExchangeAccountServiceImpl(ExchangeAccountRepository exchangeAccountRepository,
                                      ApiSecretsEncryptor apiSecretsEncryptor) {
        this.exchangeAccountRepository = exchangeAccountRepository;
        this.apiSecretsEncryptor = apiSecretsEncryptor;
    }

    @Override
    @Transactional
    public ExchangeAccountDto createExchangeAccount(CreateExchangeAccountRequest request) {
        ExchangeAccount account = ExchangeAccount.builder()
                .exchangeName(request.exchangeName().toUpperCase())
                .accountName(request.accountName())
                .encryptedApiKey(apiSecretsEncryptor.encrypt(request.apiKey()))
                .encryptedApiSecret(apiSecretsEncryptor.encrypt(request.apiSecret()))
                .allocatedCapital(request.allocatedCapital())
                .isActive(true)
                .build();

        ExchangeAccount savedAccount = exchangeAccountRepository.save(account);
        return mapToDto(savedAccount);
    }

    @Override
    public ExchangeAccountDto getExchangeAccountById(UUID id) {
        return exchangeAccountRepository.findById(id)
                .map(this::mapToDto)
                .orElseThrow(() -> new ResourceNotFoundException("Exchange account not found with id: " + id));
    }

    @Override
    public List<ExchangeAccountDto> getAllExchangeAccounts() {
        return exchangeAccountRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ExchangeAccountDto> getExchangeAccountsByExchangeName(String exchangeName) {
        return exchangeAccountRepository.findByExchangeName(exchangeName.toUpperCase()).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ExchangeAccountDto toggleAccountActiveStatus(UUID id, boolean isActive) {
        ExchangeAccount account = exchangeAccountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Exchange account not found with id: " + id));

        account.setActive(isActive);
        ExchangeAccount updated = exchangeAccountRepository.save(account);
        return mapToDto(updated);
    }

    @Override
    @Transactional
    public void deleteExchangeAccount(UUID id) {
        if (!exchangeAccountRepository.existsById(id)) {
            throw new ResourceNotFoundException("Exchange account not found with id: " + id);
        }
        exchangeAccountRepository.deleteById(id);
    }

    @Override
    public List<ExchangeAccountResponse> searchExchangeAccounts(String exchange, BigDecimal minCapital, Boolean active, Sort sort) {
        Specification<ExchangeAccount> spec = Specification.where(null);

        if (exchange != null && !exchange.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(cb.upper(root.get("exchangeName")), exchange.toUpperCase()));
        }
        if (minCapital != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("allocatedCapital"), minCapital));
        }
        if (active != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("isActive"), active));
        }

        Sort appliedSort = (sort != null && sort.isSorted()) ? sort : Sort.by(Sort.Direction.DESC, "createdAt");

        return exchangeAccountRepository.findAll(spec, appliedSort).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ExchangeAccountDto updateExchangeAccount(UUID id, CreateExchangeAccountRequest request) {
        ExchangeAccount account = exchangeAccountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Exchange account not found with id: " + id));

        account.setExchangeName(request.exchangeName().toUpperCase());
        account.setAccountName(request.accountName());
        account.setAllocatedCapital(request.allocatedCapital());

        if (request.apiKey() != null && !request.apiKey().isBlank() && !request.apiKey().startsWith("*****")) {
            account.setEncryptedApiKey(apiSecretsEncryptor.encrypt(request.apiKey()));
        }
        if (request.apiSecret() != null && !request.apiSecret().isBlank() && !request.apiSecret().startsWith("*****")) {
            account.setEncryptedApiSecret(apiSecretsEncryptor.encrypt(request.apiSecret()));
        }

        ExchangeAccount saved = exchangeAccountRepository.save(account);
        return mapToDto(saved);
    }

    private ExchangeAccountDto mapToDto(ExchangeAccount account) {
        return new ExchangeAccountDto(
                account.getId(),
                account.getExchangeName(),
                account.getAccountName(),
                apiSecretsEncryptor.decrypt(account.getEncryptedApiKey()),
                apiSecretsEncryptor.decrypt(account.getEncryptedApiSecret()),
                account.getAllocatedCapital(),
                account.isActive(),
                account.getCreatedAt()
        );
    }

    private ExchangeAccountResponse mapToResponse(ExchangeAccount account) {
        return new ExchangeAccountResponse(
                account.getId(),
                account.getExchangeName(),
                account.getAccountName(),
                account.getAllocatedCapital(),
                account.isActive(),
                account.getCreatedAt()
        );
    }
}
