package com.capitall.service.trading;

import java.math.BigDecimal;

public interface ExchangeApiClient {
    String getExchangeName();
    boolean testConnection(String apiKey, String apiSecret);
    BigDecimal getAccountBalance(String apiKey, String apiSecret);
}
