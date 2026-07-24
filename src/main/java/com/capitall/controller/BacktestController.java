package com.capitall.controller;

import com.capitall.dto.BacktestRequest;
import com.capitall.dto.BacktestResult;
import com.capitall.model.User;
import com.capitall.service.BacktestService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/backtest")
public class BacktestController {

    private final BacktestService backtestService;

    public BacktestController(BacktestService backtestService) {
        this.backtestService = backtestService;
    }

    /**
     * Run a backtest and return JSON results.
     * Called asynchronously from the frontend via fetch().
     */
    @PostMapping("/run")
    public Map<String, Object> runBacktest(
            @RequestBody BacktestRequest req,
            @AuthenticationPrincipal User user) {

        if (user == null) {
            return Map.of("success", false, "error", "Brak autoryzacji");
        }

        // Basic validation
        if (req.getSymbol() == null || req.getSymbol().isBlank()) {
            return Map.of("success", false, "error", "Symbol jest wymagany");
        }
        if (req.getStrategy() == null) {
            return Map.of("success", false, "error", "Strategia jest wymagana");
        }
        if (req.getPeriod() == null) {
            return Map.of("success", false, "error", "Okres jest wymagany");
        }
        if (req.getInitialCapital() == null || req.getInitialCapital().doubleValue() <= 0) {
            return Map.of("success", false, "error", "Kapitał startowy musi być większy od zera");
        }
        if (req.getSmaShort() >= req.getSmaLong()) {
            return Map.of("success", false, "error", "SMA krótka musi być krótsza niż SMA długa");
        }

        try {
            BacktestResult result = backtestService.run(req);
            return Map.of("success", true, "result", result);
        } catch (IllegalArgumentException e) {
            return Map.of("success", false, "error", e.getMessage());
        } catch (Exception e) {
            System.err.println("[BacktestController] Unexpected error: " + e.getMessage());
            return Map.of("success", false, "error", "Wystąpił nieoczekiwany błąd serwera.");
        }
    }
}
