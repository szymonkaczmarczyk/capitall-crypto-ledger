package com.capitall.service;

import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class SecuritiesPriceService {

    private final Map<String, String> popular = new HashMap<>();
    private final Map<String, CachedPrice> priceCache = new ConcurrentHashMap<>();
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private static final Duration CACHE_DURATION = Duration.ofSeconds(10); // Cache for 10s to avoid rate limiting

    private static class CachedPrice {
        BigDecimal price;
        Instant timestamp;
        CachedPrice(BigDecimal price, Instant timestamp) {
            this.price = price;
            this.timestamp = timestamp;
        }
    }

    public SecuritiesPriceService() {
        popular.put("AAPL", "Apple Inc. (USD)");
        popular.put("TSLA", "Tesla Inc. (USD)");
        popular.put("MSFT", "Microsoft Corp. (USD)");
        popular.put("NVDA", "NVIDIA Corp. (USD)");
        popular.put("CDR", "CD Projekt SA (PLN)");
        popular.put("PKO", "PKO BP SA (PLN)");
        popular.put("SPY", "SPDR S&P 500 ETF (USD)");
        popular.put("QQQ", "Invesco QQQ Trust (USD)");

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public BigDecimal getCurrentPrice(String symbol) {
        String sym = symbol.toUpperCase().trim();
        
        // Check cache
        CachedPrice cached = priceCache.get(sym);
        if (cached != null && Instant.now().isBefore(cached.timestamp.plus(CACHE_DURATION))) {
            return cached.price;
        }

        // Fetch from API
        BigDecimal price = fetchPriceFromYahoo(sym);
        
        if (price != null) {
            priceCache.put(sym, new CachedPrice(price, Instant.now()));
            return price;
        } else {
            // Fallback to cache if API fails
            if (cached != null) {
                return cached.price;
            }
            return new BigDecimal("100.00"); // Final fallback
        }
    }

    private BigDecimal fetchPriceFromYahoo(String symbol) {
        try {
            // Map common Polish symbols to Yahoo format
            String yahooSymbol = symbol;
            if (symbol.equals("CDR") || symbol.equals("PKO")) {
                yahooSymbol += ".WA";
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://query1.finance.yahoo.com/v8/finance/chart/" + yahooSymbol))
                    .timeout(Duration.ofSeconds(3))
                    .header("User-Agent", "Mozilla/5.0")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode resultNode = root.path("chart").path("result");
                if (resultNode.isArray() && resultNode.size() > 0) {
                    double regularMarketPrice = resultNode.get(0).path("meta").path("regularMarketPrice").asDouble();
                    return BigDecimal.valueOf(regularMarketPrice).setScale(2, RoundingMode.HALF_UP);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to fetch price for " + symbol + ": " + e.getMessage());
        }
        return null;
    }

    public Map<String, String> getPopularSymbols() {
        return popular;
    }
}
