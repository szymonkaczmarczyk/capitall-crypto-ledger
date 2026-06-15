package com.capitall.service.trading;

import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
public class BinanceApiClient implements ExchangeApiClient {

    @Override
    public String getExchangeName() {
        return "BINANCE";
    }

    @Override
    public boolean testConnection(String apiKey, String apiSecret) {

        return apiKey != null && apiSecret != null;
    }

    @Override
    public BigDecimal getAccountBalance(String apiKey, String apiSecret) {

        return BigDecimal.valueOf(10000.00);
    }
}
