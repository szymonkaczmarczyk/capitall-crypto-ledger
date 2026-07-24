package com.capitall.controller;

import com.capitall.model.SimulatedTrade;
import com.capitall.model.SimulatedWallet;
import com.capitall.model.User;
import com.capitall.service.SimulatorService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.web.bind.annotation.ResponseBody;
import java.util.Map;

@Controller
@RequestMapping("/simulator")
public class SimulatorController {

    private final SimulatorService simulatorService;
    private final com.capitall.service.SecuritiesPriceService priceService;

    public SimulatorController(SimulatorService simulatorService, com.capitall.service.SecuritiesPriceService priceService) {
        this.simulatorService = simulatorService;
        this.priceService = priceService;
    }

    @GetMapping("/price")
    @ResponseBody
    public Map<String, Object> getLivePrice(@RequestParam String symbol) {
        try {
            BigDecimal price = simulatorService.getPrice(symbol.trim());
            if (price != null && price.compareTo(BigDecimal.ZERO) > 0) {
                return Map.of("success", true, "price", price);
            }
            return Map.of("success", false, "error", "Nie udało się pobrać ceny dla: " + symbol.toUpperCase().trim());
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage() != null ? e.getMessage() : "Błąd serwera");
        }
    }

    @GetMapping
    public String showSimulator(Model model, @AuthenticationPrincipal User user) {
        if (user == null) {
            return "redirect:/login";
        }

        SimulatedWallet wallet = simulatorService.getOrCreateWallet(user.getId());
        List<SimulatorService.SimulatedHoldingSummary> holdings = simulatorService.getHoldingsSummary(user.getId());
        List<SimulatedTrade> trades = simulatorService.getTradeHistory(user.getId());

        BigDecimal cash = wallet.getBalance();
        BigDecimal assetsValue = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;

        for (SimulatorService.SimulatedHoldingSummary h : holdings) {
            assetsValue = assetsValue.add(h.getCurrentValue());
            totalCost = totalCost.add(h.getTotalCost());
        }

        BigDecimal totalValue = cash.add(assetsValue).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalPnL = BigDecimal.ZERO;
        for (SimulatorService.SimulatedHoldingSummary h : holdings) {
            totalPnL = totalPnL.add(h.getPnl());
        }
        BigDecimal totalRoi = BigDecimal.ZERO;
        if (totalCost.compareTo(BigDecimal.ZERO) > 0) {
            totalRoi = totalPnL.multiply(new BigDecimal("100")).divide(totalCost, 2, RoundingMode.HALF_UP);
        }

        model.addAttribute("active", "simulator");
        model.addAttribute("wallet", wallet);
        model.addAttribute("holdings", holdings);
        model.addAttribute("trades", trades);
        model.addAttribute("totalValue", totalValue);
        model.addAttribute("totalPnL", totalPnL);
        model.addAttribute("totalRoi", totalRoi);

        return "simulator";
    }

    @PostMapping("/trade")
    public String executeTrade(@AuthenticationPrincipal User user,
                               @RequestParam String side,
                               @RequestParam String symbol,
                               @RequestParam BigDecimal shares,
                               RedirectAttributes redirectAttributes) {
        if (user == null) {
            return "redirect:/login";
        }

        try {
            SimulatedTrade.Side tradeSide = SimulatedTrade.Side.valueOf(side.toUpperCase());
            simulatorService.executeTrade(user.getId(), tradeSide, symbol, shares);
            redirectAttributes.addFlashAttribute("successMessage", "Transakcja wykonana pomyślnie!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Wystąpił nieoczekiwany błąd podczas transakcji.");
        }

        return "redirect:/simulator";
    }

    @GetMapping("/prices")
    @ResponseBody
    public Map<String, Object> getLivePrices(@RequestParam String symbols) {
        try {
            java.util.List<String> symList = java.util.Arrays.stream(symbols.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(java.util.stream.Collectors.toList());
            Map<String, java.math.BigDecimal> results = simulatorService.getBatchPrices(symList);
            return Map.of("success", true, "prices", results);
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage() != null ? e.getMessage() : "Błąd");
        }
    }

    @PostMapping("/topup")
    public String topUpWallet(@AuthenticationPrincipal User user,
                              @RequestParam BigDecimal amount,
                              RedirectAttributes redirectAttributes) {
        if (user == null) {
            return "redirect:/login";
        }
        try {
            simulatorService.topUpWallet(user.getId(), amount);
            redirectAttributes.addFlashAttribute("successMessage", "Konto zostało doładowane kwotą $" + amount.setScale(2, RoundingMode.HALF_UP));
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Wystąpił błąd podczas doładowywania konta.");
        }
        return "redirect:/simulator";
    }

    @PostMapping("/topup-async")
    @ResponseBody
    public Map<String, Object> topUpWalletAsync(@AuthenticationPrincipal User user,
                                               @RequestParam BigDecimal amount) {
        if (user == null) {
            return Map.of("success", false, "error", "Brak autoryzacji");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return Map.of("success", false, "error", "Kwota doładowania musi być większa od zera.");
        }
        try {
            simulatorService.topUpWallet(user.getId(), amount);
            SimulatedWallet wallet = simulatorService.getOrCreateWallet(user.getId());
            return Map.of("success", true, "newBalance", wallet.getBalance());
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage() != null ? e.getMessage() : "Błąd");
        }
    }

    @PostMapping("/reset")
    public String resetPortfolio(@AuthenticationPrincipal User user, RedirectAttributes redirectAttributes) {
        if (user == null) {
            return "redirect:/login";
        }

        try {
            simulatorService.resetPortfolio(user.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Portfel demo został zresetowany.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Wystąpił błąd podczas resetowania portfela.");
        }

        return "redirect:/simulator";
    }

    @GetMapping("/historical-data")
    @ResponseBody
    public Map<String, Object> getHistoricalData(@AuthenticationPrincipal User user) {
        if (user == null) {
            return Map.of("success", false, "error", "Brak autoryzacji");
        }

        try {
            List<com.capitall.service.SimulatorService.SimulatedHoldingSummary> holdings =
                    simulatorService.getHoldingsSummary(user.getId());

            java.util.List<Map<String, Object>> datasets = new java.util.ArrayList<>();
            java.util.List<String> labels = java.util.List.of("Dzień 1", "Dzień 2", "Dzień 3", "Dzień 4", "Dzień 5", "Dzień 6", "Dzień 7");

            for (com.capitall.service.SimulatorService.SimulatedHoldingSummary h : holdings) {
                String rawSym = h.getHolding().getSymbol();
                List<BigDecimal> history = priceService.getHistoricalPrices(rawSym);
                if (history != null && !history.isEmpty()) {
                    BigDecimal firstPrice = history.get(0);
                    java.util.List<BigDecimal> pctChanges = new java.util.ArrayList<>();

                    for (BigDecimal price : history) {
                        if (firstPrice.compareTo(BigDecimal.ZERO) > 0) {
                            BigDecimal change = price.subtract(firstPrice)
                                    .multiply(new BigDecimal("100"))
                                    .divide(firstPrice, 2, RoundingMode.HALF_UP);
                            pctChanges.add(change);
                        } else {
                            pctChanges.add(BigDecimal.ZERO);
                        }
                    }

                    if (pctChanges.size() > 7) {
                        pctChanges = pctChanges.subList(pctChanges.size() - 7, pctChanges.size());
                    }
                    while (pctChanges.size() < 7) {
                        pctChanges.add(0, BigDecimal.ZERO);
                    }

                    datasets.add(Map.of(
                        "label", rawSym,
                        "data", pctChanges
                    ));
                }
            }

            return Map.of("success", true, "labels", labels, "datasets", datasets);
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage() != null ? e.getMessage() : "Błąd");
        }
    }
}
