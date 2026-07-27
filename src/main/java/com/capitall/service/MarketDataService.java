package com.capitall.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MarketDataService {

    private static final Logger log = LoggerFactory.getLogger(MarketDataService.class);
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<String, Object> marketOverviewCache = new ConcurrentHashMap<>();

    public MarketDataService() {
        // Initial placeholder until immediate post-construct fetch
        marketOverviewCache.put("fearAndGreedValue", "30");
        marketOverviewCache.put("fearAndGreedClassification", "Fear");
        marketOverviewCache.put("btcDominance", "56.4%");
        marketOverviewCache.put("totalMarketCapUsd", "$2.48 Trillion");
    }

    @PostConstruct
    public void init() {
        fetchFearAndGreedIndex();
        fetchGlobalMarketStats();
    }

    public Map<String, Object> getMarketOverview() {
        return new HashMap<>(marketOverviewCache);
    }

    @Scheduled(fixedRate = 120000) // Co 2 minuty
    public void fetchFearAndGreedIndex() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.alternative.me/fng/"))
                    .header("User-Agent", "CapitallApp/1.0")
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(res.body());
                if (root.has("data") && root.get("data").isArray() && root.get("data").size() > 0) {
                    JsonNode data = root.get("data").get(0);
                    String val = data.get("value").asText();
                    String classifEn = data.get("value_classification").asText();
                    String classifPl = translateClassification(classifEn);

                    marketOverviewCache.put("fearAndGreedValue", val);
                    marketOverviewCache.put("fearAndGreedClassification", classifPl);
                    log.info("[MarketDataService] Zaktualizowano natychmiastowo Fear & Greed Index API: {} ({})", val, classifPl);
                }
            }
        } catch (Exception e) {
            log.warn("[MarketDataService] Błąd pobierania Fear & Greed Index API: {}", e.getMessage());
        }
    }

    @Scheduled(fixedRate = 180000) // Co 3 minuty
    public void fetchGlobalMarketStats() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.coingecko.com/api/v3/global"))
                    .header("User-Agent", "CapitallApp/1.0")
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(res.body());
                if (root.has("data")) {
                    JsonNode data = root.get("data");
                    if (data.has("market_cap_percentage") && data.get("market_cap_percentage").has("btc")) {
                        double btcDom = data.get("market_cap_percentage").get("btc").asDouble();
                        marketOverviewCache.put("btcDominance", String.format("%.1f%%", btcDom));
                    }
                    if (data.has("total_market_cap") && data.get("total_market_cap").has("usd")) {
                        double capUsd = data.get("total_market_cap").get("usd").asDouble();
                        marketOverviewCache.put("totalMarketCapUsd", String.format("$%.2f Trillion", capUsd / 1e12));
                    }
                    log.info("[MarketDataService] Zaktualizowano statystyki globalne z CoinGecko API: Dominacja BTC = {}", marketOverviewCache.get("btcDominance"));
                }
            }
        } catch (Exception e) {
            log.debug("[MarketDataService] CoinGecko API offline or rate-limited: {}", e.getMessage());
        }
    }

    private String translateClassification(String en) {
        if (en == null) return "Neutralnie";
        switch (en.toLowerCase()) {
            case "extreme fear": return "Ekstremalny Strach";
            case "fear": return "Strach";
            case "neutral": return "Neutralnie";
            case "greed": return "Chciwość";
            case "extreme greed": return "Ekstremalna Chciwość";
            default: return en;
        }
    }
}
