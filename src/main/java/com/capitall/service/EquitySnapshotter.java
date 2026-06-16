package com.capitall.service;

import com.capitall.model.EquitySnapshot;
import com.capitall.model.Holding;
import com.capitall.model.Wallet;
import com.capitall.repository.EquitySnapshotRepository;
import com.capitall.repository.HoldingRepository;
import com.capitall.repository.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class EquitySnapshotter {

    private static final Logger log = LoggerFactory.getLogger(EquitySnapshotter.class);

    private static final List<String> BINANCE_HOSTS = List.of(
            "api.binance.com",
            "api1.binance.com",
            "data-api.binance.vision"
    );

    private static final Pattern PRICE_ENTRY =
            Pattern.compile("\"symbol\"\\s*:\\s*\"([A-Z0-9]+)\"\\s*,\\s*\"price\"\\s*:\\s*\"([0-9.]+)\"");

    private final WalletRepository walletRepository;
    private final HoldingRepository holdingRepository;
    private final EquitySnapshotRepository snapshotRepository;
    private final HttpClient httpClient;

    public EquitySnapshotter(WalletRepository walletRepository,
                             HoldingRepository holdingRepository,
                             EquitySnapshotRepository snapshotRepository) {
        this.walletRepository = walletRepository;
        this.holdingRepository = holdingRepository;
        this.snapshotRepository = snapshotRepository;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(4))
                .build();
    }

    @Scheduled(fixedRateString = "${capitall.scheduler.equity-snapshot-ms:300000}")
    public void snapshotAllUsers() {
        try {
            List<Wallet> wallets = walletRepository.findAll();
            if (wallets.isEmpty()) return;

            Set<String> neededPairs = new HashSet<>();
            for (Holding h : holdingRepository.findAll()) {
                neededPairs.add(h.getSymbol() + "USDT");
            }
            Map<String, BigDecimal> prices = neededPairs.isEmpty() ? Map.of() : fetchPrices(neededPairs);

            for (Wallet w : wallets) {
                snapshotUserInternal(w, prices);
            }
            log.info("Captured equity snapshots for {} users", wallets.size());
        } catch (Exception e) {
            log.error("Equity snapshotter failed", e);
        }
    }

    @Transactional
    public void snapshotUser(UUID userId) {
        Wallet w = walletRepository.findByUserId(userId).orElse(null);
        if (w == null) return;
        List<Holding> holdings = holdingRepository.findByUserId(userId);
        Set<String> pairs = new HashSet<>();
        for (Holding h : holdings) pairs.add(h.getSymbol() + "USDT");
        Map<String, BigDecimal> prices = pairs.isEmpty() ? Map.of() : fetchPrices(pairs);
        snapshotUserInternal(w, prices);
    }

    @Transactional
    protected void snapshotUserInternal(Wallet w, Map<String, BigDecimal> prices) {
        BigDecimal cash = w.getUsdBalance() == null ? BigDecimal.ZERO : w.getUsdBalance();
        BigDecimal crypto = BigDecimal.ZERO;
        for (Holding h : holdingRepository.findByUserId(w.getUserId())) {
            BigDecimal price = prices.get(h.getSymbol() + "USDT");
            if (price == null) continue;
            crypto = crypto.add(h.getAmount().multiply(price));
        }
        crypto = crypto.setScale(2, RoundingMode.HALF_UP);
        BigDecimal equity = cash.add(crypto).setScale(2, RoundingMode.HALF_UP);

        EquitySnapshot s = new EquitySnapshot();
        s.setUserId(w.getUserId());
        s.setCashUsd(cash.setScale(2, RoundingMode.HALF_UP));
        s.setCryptoUsd(crypto);
        s.setEquityUsd(equity);
        snapshotRepository.save(s);
    }

    private Map<String, BigDecimal> fetchPrices(Set<String> pairs) {
        String symbolsParam = "[" + String.join(",", pairs.stream().map(p -> "\"" + p + "\"").toList()) + "]";
        String encoded = java.net.URLEncoder.encode(symbolsParam, java.nio.charset.StandardCharsets.UTF_8);
        for (String host : BINANCE_HOSTS) {
            String url = "https://" + host + "/api/v3/ticker/price?symbols=" + encoded;
            try {
                HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                        .timeout(Duration.ofSeconds(5))
                        .GET()
                        .build();
                HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
                if (res.statusCode() != 200) continue;
                Map<String, BigDecimal> out = new HashMap<>();
                Matcher m = PRICE_ENTRY.matcher(res.body());
                while (m.find()) {
                    out.put(m.group(1), new BigDecimal(m.group(2)));
                }
                if (!out.isEmpty()) return out;
            } catch (Exception e) {
                log.debug("Binance host {} failed: {}", host, e.getMessage());
            }
        }
        log.warn("All Binance hosts failed for equity snapshot");
        return Map.of();
    }
}
