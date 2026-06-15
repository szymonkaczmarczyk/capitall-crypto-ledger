package com.capitall.service.trading;

import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ExchangeApiClientFactory {

    private final Map<String, ExchangeApiClient> clientsByExchangeName;

    public ExchangeApiClientFactory(List<ExchangeApiClient> clients) {
        this.clientsByExchangeName = clients.stream()
                .collect(Collectors.toMap(
                        client -> client.getExchangeName().toUpperCase(),
                        Function.identity()
                ));
    }

    public ExchangeApiClient getClient(String exchangeName) {
        ExchangeApiClient client = clientsByExchangeName.get(exchangeName.toUpperCase());
        if (client == null) {
            throw new IllegalArgumentException("Unsupported exchange: " + exchangeName);
        }
        return client;
    }
}
