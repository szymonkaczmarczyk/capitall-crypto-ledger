package com.capitall.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ExchangeRateService {

    // Base rates representing rate in PLN (e.g., 1 USD = 4.0 PLN)
    private final Map<String, BigDecimal> ratesToPln = new ConcurrentHashMap<>();
    private final RestTemplate restTemplate = new RestTemplate();
    private long lastFetchedTime = 0;
    private static final long CACHE_DURATION_MS = 3600000; // 1 hour caching

    public ExchangeRateService() {
        initializeFallbackRates();
    }

    private void initializeFallbackRates() {
        ratesToPln.put("PLN", BigDecimal.ONE);
        ratesToPln.put("USD", new BigDecimal("4.00"));
        ratesToPln.put("EUR", new BigDecimal("4.30"));
        ratesToPln.put("GBP", new BigDecimal("5.10"));
    }

    private synchronized void fetchRatesIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastFetchedTime < CACHE_DURATION_MS) {
            return;
        }
        try {
            String url = "https://api.nbp.pl/api/exchangerates/tables/A?format=json";
            Map[] response = restTemplate.getForObject(url, Map[].class);
            if (response != null && response.length > 0 && response[0] != null) {
                List<Map<String, Object>> ratesList = (List<Map<String, Object>>) response[0].get("rates");
                if (ratesList != null) {
                    ratesToPln.put("PLN", BigDecimal.ONE);
                    for (Map<String, Object> rate : ratesList) {
                        String code = (String) rate.get("code");
                        Object midObj = rate.get("mid");
                        if (code != null && midObj != null) {
                            BigDecimal midVal = new BigDecimal(midObj.toString());
                            if ("USD".equalsIgnoreCase(code) || "EUR".equalsIgnoreCase(code) || "GBP".equalsIgnoreCase(code)) {
                                ratesToPln.put(code.toUpperCase(), midVal);
                            }
                        }
                    }
                    lastFetchedTime = now;
                }
            }
        } catch (Exception e) {
            // Log warning and keep using existing rates / fallback
            System.err.println("NBP API fetch failed. Using fallback rates. Error: " + e.getMessage());
        }
    }

    public BigDecimal getRateToPln(String currency) {
        fetchRatesIfNeeded();
        BigDecimal rate = ratesToPln.get(currency.toUpperCase());
        if (rate == null) {
            throw new IllegalArgumentException("Unsupported currency: " + currency);
        }
        return rate;
    }

    /**
     * Converts an amount from one currency to another using mid market NBP rates
     */
    public BigDecimal convert(BigDecimal amount, String from, String to) {
        if (amount == null) return BigDecimal.ZERO;
        if (from.equalsIgnoreCase(to)) return amount;

        BigDecimal fromRate = getRateToPln(from);
        BigDecimal toRate = getRateToPln(to);

        // Convert from -> PLN -> to
        BigDecimal amountInPln = amount.multiply(fromRate);
        return amountInPln.divide(toRate, 4, RoundingMode.HALF_UP);
    }

    /**
     * Returns a list of rates for the Exchange panel, with buying/selling rates including a simulated spread.
     * Spread is 0.8% (0.008)
     */
    public List<CurrencyRateInfo> getExchangeRatesList() {
        fetchRatesIfNeeded();
        List<CurrencyRateInfo> displayList = new ArrayList<>();
        BigDecimal spread = new BigDecimal("0.008");

        for (String code : List.of("USD", "EUR", "GBP")) {
            BigDecimal mid = getRateToPln(code);
            BigDecimal buyRate = mid.subtract(mid.multiply(spread)).setScale(4, RoundingMode.HALF_UP);
            BigDecimal sellRate = mid.add(mid.multiply(spread)).setScale(4, RoundingMode.HALF_UP);
            displayList.add(new CurrencyRateInfo(code, mid, buyRate, sellRate));
        }

        return displayList;
    }

    public static class CurrencyRateInfo {
        private final String code;
        private final BigDecimal midRate;
        private final BigDecimal buyRate;  // rate at which user buys this currency (using PLN)
        private final BigDecimal sellRate; // rate at which user sells this currency (obtaining PLN)

        public CurrencyRateInfo(String code, BigDecimal midRate, BigDecimal buyRate, BigDecimal sellRate) {
            this.code = code;
            this.midRate = midRate;
            this.buyRate = buyRate;
            this.sellRate = sellRate;
        }

        public String getCode() { return code; }
        public BigDecimal getMidRate() { return midRate; }
        public BigDecimal getBuyRate() { return buyRate; }
        public BigDecimal getSellRate() { return sellRate; }
    }
}
