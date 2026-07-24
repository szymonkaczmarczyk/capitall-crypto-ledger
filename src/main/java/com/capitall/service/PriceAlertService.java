package com.capitall.service;

import com.capitall.model.PriceAlert;
import com.capitall.model.PriceAlertLog;
import com.capitall.repository.PriceAlertLogRepository;
import com.capitall.repository.PriceAlertRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PriceAlertService {

    private final PriceAlertRepository alertRepository;
    private final PriceAlertLogRepository logRepository;
    private final SecuritiesPriceService priceService;
    private final HttpClient httpClient;

    public PriceAlertService(PriceAlertRepository alertRepository,
                             PriceAlertLogRepository logRepository,
                             SecuritiesPriceService priceService) {
        this.alertRepository = alertRepository;
        this.logRepository = logRepository;
        this.priceService = priceService;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    @Transactional(readOnly = true)
    public List<PriceAlert> getActiveAlerts(UUID userId) {
        return alertRepository.findByUserIdAndTriggeredFalseOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<PriceAlertLog> getLogsForUser(UUID userId) {
        return logRepository.findByUserIdOrderByOccurredAtDesc(userId);
    }

    @Transactional
    public PriceAlert createAlert(UUID userId, String assetType, String symbol,
                                  String conditionType, BigDecimal targetPrice, boolean recurring) {
        if (targetPrice == null || targetPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Cena docelowa musi być większa od zera.");
        }
        PriceAlert alert = new PriceAlert(userId, assetType, symbol.toUpperCase(),
                conditionType.toUpperCase(), targetPrice, recurring);
        alertRepository.save(alert);

        String details = String.format("%s %s @ $%s (%s)",
                conditionType.equalsIgnoreCase("ABOVE") ? "powyżej" : "poniżej",
                symbol.toUpperCase(), targetPrice.toPlainString(),
                recurring ? "cykliczny" : "jednorazowy");
        logRepository.save(new PriceAlertLog(userId, PriceAlertLog.Action.CREATED,
                symbol, assetType, details));
        return alert;
    }

    @Transactional
    public void updateAlert(UUID id, UUID userId, String conditionType,
                            BigDecimal targetPrice, boolean recurring) {
        PriceAlert alert = alertRepository.findById(id).orElse(null);
        if (alert == null || !alert.getUserId().equals(userId)) return;
        if (targetPrice == null || targetPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Cena docelowa musi być większa od zera.");
        }

        String details = String.format(
                "%s: zmieniono z [%s @ $%s, %s] na [%s @ $%s, %s]",
                alert.getSymbol(),
                alert.getConditionType().equalsIgnoreCase("ABOVE") ? "powyżej" : "poniżej",
                alert.getTargetPrice().toPlainString(),
                alert.isRecurring() ? "cykliczny" : "jednorazowy",
                conditionType.equalsIgnoreCase("ABOVE") ? "powyżej" : "poniżej",
                targetPrice.toPlainString(),
                recurring ? "cykliczny" : "jednorazowy");

        alert.setConditionType(conditionType.toUpperCase());
        alert.setTargetPrice(targetPrice);
        alert.setRecurring(recurring);
        alert.setLastTriggeredAt(null);
        alertRepository.save(alert);

        logRepository.save(new PriceAlertLog(userId, PriceAlertLog.Action.EDITED,
                alert.getSymbol(), alert.getAssetType(), details));
    }

    @Transactional
    public void deleteAlert(UUID id, UUID userId) {
        PriceAlert alert = alertRepository.findById(id).orElse(null);
        if (alert == null || !alert.getUserId().equals(userId)) return;

        String details = String.format("%s %s @ $%s",
                alert.getConditionType().equalsIgnoreCase("ABOVE") ? "powyżej" : "poniżej",
                alert.getSymbol(), alert.getTargetPrice().toPlainString());
        logRepository.save(new PriceAlertLog(userId, PriceAlertLog.Action.DELETED,
                alert.getSymbol(), alert.getAssetType(), details));

        alertRepository.delete(alert);
    }

    @Transactional
    public List<PriceAlert> checkAndTriggerAlertsForUser(UUID userId) {
        List<PriceAlert> activeAlerts = alertRepository.findByUserIdAndTriggeredFalseOrderByCreatedAtDesc(userId);
        List<PriceAlert> fired = new ArrayList<>();
        if (activeAlerts.isEmpty()) return fired;

        Set<String> cryptoSymbols = new HashSet<>();
        for (PriceAlert a : activeAlerts) {
            if ("CRYPTO".equalsIgnoreCase(a.getAssetType())) {
                cryptoSymbols.add(a.getSymbol().toUpperCase());
            }
        }
        Map<String, BigDecimal> cryptoPrices = fetchBinancePrices(cryptoSymbols);
        LocalDate today = LocalDate.now();

        for (PriceAlert alert : activeAlerts) {
            BigDecimal currentPrice = null;
            String symbol = alert.getSymbol().toUpperCase();

            if ("CRYPTO".equalsIgnoreCase(alert.getAssetType())) {
                currentPrice = cryptoPrices.get(symbol);
            } else if ("STOCK".equalsIgnoreCase(alert.getAssetType())) {
                try { currentPrice = priceService.getCurrentPrice(symbol); }
                catch (Exception ignored) {}
            }

            if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) <= 0) continue;

            boolean conditionMet = false;
            if ("ABOVE".equalsIgnoreCase(alert.getConditionType())) {
                conditionMet = currentPrice.compareTo(alert.getTargetPrice()) >= 0;
            } else if ("BELOW".equalsIgnoreCase(alert.getConditionType())) {
                conditionMet = currentPrice.compareTo(alert.getTargetPrice()) <= 0;
            }

            if (!conditionMet) continue;

            if (alert.isRecurring()) {
                LocalDate lastFiredDay = alert.getLastTriggeredAt() != null
                        ? alert.getLastTriggeredAt().toLocalDate() : null;
                if (lastFiredDay != null && lastFiredDay.equals(today)) {
                    continue;
                }
                alert.setLastTriggeredAt(LocalDateTime.now());
                alertRepository.save(alert);
            } else {
                alert.setTriggered(true);
                alert.setLastTriggeredAt(LocalDateTime.now());
                alertRepository.save(alert);
            }

            logRepository.save(new PriceAlertLog(userId, PriceAlertLog.Action.TRIGGERED,
                    symbol, alert.getAssetType(),
                    String.format("Cena $%.2f %s próg $%s",
                            currentPrice, alert.isRecurring() ? "(cykliczny)" : "(jednorazowy)",
                            alert.getTargetPrice().toPlainString())));
            fired.add(alert);
        }

        return fired;
    }

    private Map<String, BigDecimal> fetchBinancePrices(Set<String> symbols) {
        if (symbols.isEmpty()) return Map.of();
        Map<String, BigDecimal> prices = new HashMap<>();
        try {
            List<String> pairs = symbols.stream().map(s -> s + "USDT").toList();
            String symbolsParam = "[" + String.join(",", pairs.stream().map(p -> "\"" + p + "\"").toList()) + "]";
            String encoded = java.net.URLEncoder.encode(symbolsParam, java.nio.charset.StandardCharsets.UTF_8);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.binance.com/api/v3/ticker/price?symbols=" + encoded))
                    .build();
            HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() == 200) {
                Pattern pattern = Pattern.compile("\"symbol\"\\s*:\\s*\"([A-Z0-9]+)\"\\s*,\\s*\"price\"\\s*:\\s*\"([0-9.]+)\"");
                Matcher m = pattern.matcher(res.body());
                while (m.find()) {
                    String pair = m.group(1);
                    prices.put(pair.replace("USDT", ""), new BigDecimal(m.group(2)));
                }
            }
        } catch (Exception ignored) {}
        return prices;
    }
}
