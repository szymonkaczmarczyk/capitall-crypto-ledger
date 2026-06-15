package com.capitall.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.capitall.dto.CreateExchangeAccountRequest;
import com.capitall.dto.ExchangeAccountDto;
import com.capitall.dto.ExchangeAccountResponse;
import com.capitall.service.ExchangeAccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AssetController.class, excludeAutoConfiguration = {
    org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
    org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
})
class AssetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExchangeAccountService exchangeAccountService;

    @MockBean
    private com.capitall.config.MaintenanceModeState maintenanceModeState;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createAsset_ShouldReturnCreatedAndHideKeys() throws Exception {
        CreateExchangeAccountRequest request = new CreateExchangeAccountRequest(
                "BINANCE", "My Account", "my-api-key", "my-api-secret", BigDecimal.valueOf(1500)
        );

        UUID generatedId = UUID.randomUUID();
        ExchangeAccountDto dto = new ExchangeAccountDto(
                generatedId, "BINANCE", "My Account", "my-api-key", "my-api-secret",
                BigDecimal.valueOf(1500), true, LocalDateTime.now()
        );

        when(exchangeAccountService.createExchangeAccount(any(CreateExchangeAccountRequest.class)))
                .thenReturn(dto);

        mockMvc.perform(post("/api/assets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(generatedId.toString()))
                .andExpect(jsonPath("$.exchangeName").value("BINANCE"))
                .andExpect(jsonPath("$.accountName").value("My Account"))
                .andExpect(jsonPath("$.allocatedCapital").value(1500))
                .andExpect(jsonPath("$.apiKey").doesNotExist()) 
                .andExpect(jsonPath("$.apiSecret").doesNotExist());
    }

    @Test
    void createAsset_ShouldReturnBadRequest_WhenValidationFails() throws Exception {
        CreateExchangeAccountRequest invalidRequest = new CreateExchangeAccountRequest(
                "", "", "key", "secret", BigDecimal.valueOf(-10)
        );

        mockMvc.perform(post("/api/assets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors.exchangeName").exists())
                .andExpect(jsonPath("$.validationErrors.accountName").exists())
                .andExpect(jsonPath("$.validationErrors.allocatedCapital").exists());
    }

    @Test
    void searchAssets_ShouldCallSearchWithParamsAndReturnSafeList() throws Exception {
        UUID id = UUID.randomUUID();
        ExchangeAccountResponse response = new ExchangeAccountResponse(
                id, "BINANCE", "Binance Bot", BigDecimal.valueOf(25000), true, LocalDateTime.now()
        );

        when(exchangeAccountService.searchExchangeAccounts(eq("BINANCE"), eq(BigDecimal.valueOf(10000)), eq(true), any(Sort.class)))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/assets")
                .param("exchange", "BINANCE")
                .param("minCapital", "10000")
                .param("active", "true")
                .param("sort", "allocatedCapital,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(id.toString()))
                .andExpect(jsonPath("$[0].exchangeName").value("BINANCE"))
                .andExpect(jsonPath("$[0].apiKey").doesNotExist()); 
    }
}
