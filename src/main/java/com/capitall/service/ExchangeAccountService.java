package com.capitall.service;

import com.capitall.dto.CreateExchangeAccountRequest;
import com.capitall.dto.ExchangeAccountDto;
import com.capitall.dto.ExchangeAccountResponse;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface ExchangeAccountService {
    ExchangeAccountDto createExchangeAccount(CreateExchangeAccountRequest request);

    ExchangeAccountDto getExchangeAccountById(UUID id);

    List<ExchangeAccountDto> getAllExchangeAccounts();

    List<ExchangeAccountDto> getExchangeAccountsByExchangeName(String exchangeName);

    ExchangeAccountDto toggleAccountActiveStatus(UUID id, boolean isActive);

    void deleteExchangeAccount(UUID id);

    List<ExchangeAccountResponse> searchExchangeAccounts(String exchange, BigDecimal minCapital, Boolean active,
            Sort sort);

    ExchangeAccountDto updateExchangeAccount(UUID id, CreateExchangeAccountRequest request);
}
