package com.capitall.service;

import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.*;
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
    private final ConcurrentHashMap<String, CachedPrice> priceCache = new ConcurrentHashMap<>();
    private final Set<String> refreshingSymbols = ConcurrentHashMap.newKeySet();
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private static final Duration CACHE_TTL = Duration.ofSeconds(6);
    private static final int FETCH_TIMEOUT_SECONDS = 5;

    private static final List<String> WARMUP_SYMBOLS = List.of(
        "BTC-USD", "ETH-USD", "SOL-USD", "ADA-USD", "DOT-USD", "XRP-USD",
        "DOGE-USD", "LTC-USD", "LINK-USD", "BNB-USD", "AVAX-USD", "POL-USD", "SHIB-USD",
        "AAPL", "TSLA", "MSFT", "NVDA", "GOOGL", "AMZN", "META",
        "NFLX", "AMD", "INTC", "BABA", "NIO",
        "CDR.WA", "PKO.WA", "PKN.WA", "KGH.WA", "LPP.WA", "ALE.WA", "DNP.WA",
        "SPY", "QQQ"
    );

    private static final Set<String> WSE_SHORT = Set.of("CDR","PKO","PKN","KGH","LPP","ALE","DNP","PZU","JSW","ALR","MBK","OPL");
    private static final Set<String> CRYPTO_SHORT = Set.of(
        "BTC","ETH","SOL","ADA","DOT","XRP","DOGE","LTC","LINK","BNB","AVAX","POL","SHIB"
    );

    private static class CachedPrice {
        final BigDecimal price;
        final Instant timestamp;
        CachedPrice(BigDecimal price) {
            this.price = price;
            this.timestamp = Instant.now();
        }
        boolean isExpired() {
            return Instant.now().isAfter(timestamp.plus(CACHE_TTL));
        }
    }

    public SecuritiesPriceService() {
        popular.put("AAPL", "Apple Inc. (USD)");
        popular.put("TSLA", "Tesla Inc. (USD)");
        popular.put("MSFT", "Microsoft Corp. (USD)");
        popular.put("NVDA", "NVIDIA Corp. (USD)");
        popular.put("CDR", "CD Projekt SA (PLN)");
        popular.put("PKO", "PKO BP SA (PLN)");
        popular.put("PKN", "Orlen SA (PLN)");
        popular.put("KGH", "KGHM Polska Miedź SA (PLN)");
        popular.put("LPP", "LPP SA (PLN)");
        popular.put("SPY", "SPDR S&P 500 ETF (USD)");
        popular.put("QQQ", "Invesco QQQ Trust (USD)");

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .executor(Executors.newFixedThreadPool(16))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void warmUpCache() {
        System.out.println("[PriceService] Warm-up started for " + WARMUP_SYMBOLS.size() + " symbols...");
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (String sym : WARMUP_SYMBOLS) {
            CompletableFuture<Void> f = CompletableFuture.runAsync(() -> {
                BigDecimal p = fetchPriceFromYahoo(sym);
                if (p != null) {
                    priceCache.put(sym, new CachedPrice(p));
                    System.out.println("[PriceService] Warm-up OK: " + sym + " = " + p);
                } else {
                    System.err.println("[PriceService] Warm-up FAILED: " + sym);
                }
            });
            futures.add(f);
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenRun(() -> System.out.println("[PriceService] Warm-up complete."));
    }

    public BigDecimal getCurrentPrice(String rawSymbol) {
        String sym = toYahooSymbol(rawSymbol);
        CachedPrice cached = priceCache.get(sym);

        if (cached != null) {
            if (cached.isExpired()) {
                triggerBackgroundRefresh(sym);
            }
            return cached.price;
        }

        BigDecimal price = fetchPriceFromYahoo(sym);
        if (price != null) {
            priceCache.put(sym, new CachedPrice(price));
        }
        return price;
    }

    public Map<String, BigDecimal> getBatchPrices(List<String> rawSymbols) {
        Map<String, BigDecimal> result = new ConcurrentHashMap<>();
        List<CompletableFuture<Void>> pending = new ArrayList<>();

        for (String raw : rawSymbols) {
            String sym = toYahooSymbol(raw);
            String key = raw.toUpperCase().trim();
            CachedPrice cached = priceCache.get(sym);
            if (cached == null) {
                cached = priceCache.get(key);
            }

            if (cached != null) {
                if (cached.isExpired()) triggerBackgroundRefresh(sym);
                putPriceWithAliases(result, key, sym, cached.price);
            } else {
                CompletableFuture<Void> f = CompletableFuture.runAsync(() -> {
                    BigDecimal p = fetchPriceFromYahoo(sym);
                    if (p != null) {
                        priceCache.put(sym, new CachedPrice(p));
                        putPriceWithAliases(result, key, sym, p);
                    }
                });
                pending.add(f);
            }
        }

        if (!pending.isEmpty()) {
            try {
                CompletableFuture.allOf(pending.toArray(new CompletableFuture[0]))
                        .get(FETCH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (Exception ignored) {  }
        }

        return result;
    }

    private void putPriceWithAliases(Map<String, BigDecimal> map, String rawKey, String yahooSym, BigDecimal price) {
        if (price == null) return;
        String rawClean = rawKey.toUpperCase().trim();
        map.put(rawClean, price);
        map.put(yahooSym, price);

        if (yahooSym.endsWith("-USD")) {
            map.put(yahooSym.substring(0, yahooSym.length() - 4), price);
        }
        if (yahooSym.endsWith(".WA")) {
            map.put(yahooSym.substring(0, yahooSym.length() - 3), price);
        }
    }

    public String toYahooSymbol(String raw) {
        String sym = raw.toUpperCase().trim();
        if (CRYPTO_SHORT.contains(sym)) return sym + "-USD";
        if (WSE_SHORT.contains(sym))    return sym + ".WA";
        return sym;
    }

    private void triggerBackgroundRefresh(String yahooSym) {
        if (refreshingSymbols.add(yahooSym)) {
            CompletableFuture.runAsync(() -> {
                try {
                    BigDecimal p = fetchPriceFromYahoo(yahooSym);
                    if (p != null) priceCache.put(yahooSym, new CachedPrice(p));
                } finally {
                    refreshingSymbols.remove(yahooSym);
                }
            });
        }
    }

    BigDecimal fetchPriceFromYahoo(String yahooSymbol) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://query1.finance.yahoo.com/v8/finance/chart/" + yahooSymbol))
                    .timeout(Duration.ofSeconds(FETCH_TIMEOUT_SECONDS))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                    .header("Accept", "application/json")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode resultNode = root.path("chart").path("result");
                if (resultNode.isArray() && resultNode.size() > 0) {
                    double price = resultNode.get(0).path("meta").path("regularMarketPrice").asDouble();
                    if (price > 0) {
                        return BigDecimal.valueOf(price).setScale(2, RoundingMode.HALF_UP);
                    }
                }
            } else {
                System.err.println("[PriceService] HTTP " + response.statusCode() + " for " + yahooSymbol);
            }
        } catch (Exception e) {
            System.err.println("[PriceService] Fetch error for " + yahooSymbol + ": " + e.getMessage());
        }
        return null;
    }

    public Map<String, String> getPopularSymbols() {
        return popular;
    }

    /**
     * Fetches OHLCV bars for backtesting.
     * @param rawSymbol  user-facing symbol (e.g. "BTC-USD", "AAPL", "CDR")
     * @param range      Yahoo Finance range param, e.g. "1mo", "3mo", "6mo", "1y", "2y"
     * @param interval   Yahoo Finance interval, e.g. "1d", "1wk"
     * @return list of OHLCVBar objects ordered oldest→newest
     */
    public List<com.capitall.dto.BacktestResult.OHLCVBar> fetchOHLCV(String rawSymbol, String range, String interval) {
        String sym = toYahooSymbol(rawSymbol);
        List<com.capitall.dto.BacktestResult.OHLCVBar> bars = new ArrayList<>();
        try {
            String url = "https://query1.finance.yahoo.com/v8/finance/chart/" + sym
                    + "?range=" + range + "&interval=" + interval;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                System.err.println("[PriceService] OHLCV HTTP " + response.statusCode() + " for " + sym);
                return bars;
            }

            JsonNode root       = objectMapper.readTree(response.body());
            JsonNode resultNode = root.path("chart").path("result");
            if (!resultNode.isArray() || resultNode.size() == 0) return bars;

            JsonNode result     = resultNode.get(0);
            JsonNode timestamps = result.path("timestamp");
            JsonNode quote      = result.path("indicators").path("quote").get(0);

            if (timestamps == null || !timestamps.isArray() || quote == null) return bars;

            JsonNode opens  = quote.path("open");
            JsonNode highs  = quote.path("high");
            JsonNode lows   = quote.path("low");
            JsonNode closes = quote.path("close");
            JsonNode volumes= quote.path("volume");

            for (int i = 0; i < timestamps.size(); i++) {
                double close = closes.get(i).asDouble(0);
                if (close <= 0) continue; // skip null/invalid bars

                long epochSec = timestamps.get(i).asLong();
                String date = java.time.Instant.ofEpochSecond(epochSec)
                        .atZone(java.time.ZoneOffset.UTC)
                        .toLocalDate()
                        .toString();

                bars.add(new com.capitall.dto.BacktestResult.OHLCVBar(
                        date,
                        opens.get(i).asDouble(close),
                        highs.get(i).asDouble(close),
                        lows.get(i).asDouble(close),
                        close,
                        volumes.get(i).asLong(0)
                ));
            }
        } catch (Exception e) {
            System.err.println("[PriceService] OHLCV fetch error for " + sym + ": " + e.getMessage());
        }
        return bars;
    }

    public List<BigDecimal> getHistoricalPrices(String rawSymbol) {
        String sym = toYahooSymbol(rawSymbol);
        List<BigDecimal> prices = new ArrayList<>();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://query1.finance.yahoo.com/v8/finance/chart/" + sym + "?range=7d&interval=1d"))
                    .timeout(Duration.ofSeconds(FETCH_TIMEOUT_SECONDS))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode resultNode = root.path("chart").path("result");
                if (resultNode.isArray() && resultNode.size() > 0) {
                    JsonNode indicators = resultNode.get(0).path("indicators");
                    JsonNode quote = indicators.path("quote");
                    JsonNode closeNode = null;
                    if (quote.isArray() && quote.size() > 0) {
                        closeNode = quote.get(0).path("close");
                    }
                    if (closeNode != null && closeNode.isArray()) {
                        for (JsonNode priceNode : closeNode) {
                            if (priceNode.isNumber() && priceNode.asDouble() > 0) {
                                prices.add(BigDecimal.valueOf(priceNode.asDouble()).setScale(2, RoundingMode.HALF_UP));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[PriceService] History fetch error for " + sym + ": " + e.getMessage());
        }
        return prices;
    }
}
