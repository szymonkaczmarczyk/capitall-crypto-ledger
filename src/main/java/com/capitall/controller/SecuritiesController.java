package com.capitall.controller;

import com.capitall.model.SecurityTrade;
import com.capitall.model.User;
import com.capitall.service.SecuritiesPriceService;
import com.capitall.service.SecurityPortfolioService;
import com.capitall.service.WalletService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Controller
public class SecuritiesController {

    private final SecurityPortfolioService portfolioService;
    private final SecuritiesPriceService priceService;
    private final WalletService walletService;

    public SecuritiesController(SecurityPortfolioService portfolioService,
                                SecuritiesPriceService priceService,
                                WalletService walletService) {
        this.portfolioService = portfolioService;
        this.priceService = priceService;
        this.walletService = walletService;
    }

    @GetMapping("/securities")
    public String viewSecurities(Model model, @AuthenticationPrincipal User user) {
        if (user == null) {
            return "redirect:/login";
        }

        model.addAttribute("stats", portfolioService.getPortfolioSummary(user.getId()));
        model.addAttribute("items", portfolioService.getPortfolioItems(user.getId()));
        model.addAttribute("trades", portfolioService.getTradeHistory(user.getId()));
        model.addAttribute("popularSymbols", priceService.getPopularSymbols());
        model.addAttribute("traderName", user.getUsername());
        model.addAttribute("wallet", walletService.getOrCreate(user.getId()));
        model.addAttribute("totalUsdValuation", walletService.getTotalValuationInUsd(user.getId()));

        return "securities";
    }

    @PostMapping("/securities/trade")
    public String executeTrade(@AuthenticationPrincipal User user,
                               @RequestParam SecurityTrade.Side side,
                               @RequestParam String symbol,
                               @RequestParam String name,
                               @RequestParam BigDecimal shares,
                               @RequestParam BigDecimal price,
                               @RequestParam BigDecimal fee,
                               @RequestParam String currency,
                               RedirectAttributes redirectAttributes) {
        if (user == null) {
            return "redirect:/login";
        }

        try {
            portfolioService.executeTrade(user.getId(), side, symbol, name, shares, price, fee, currency);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Pomyślnie zarejestrowano transakcję " + (side == SecurityTrade.Side.BUY ? "kupna" : "sprzedaży") + " " + symbol.toUpperCase());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Błąd transakcji: " + e.getMessage());
        }

        return "redirect:/securities";
    }

    @GetMapping("/securities/data")
    @org.springframework.web.bind.annotation.ResponseBody
    public Map<String, Object> getSecuritiesData(@AuthenticationPrincipal User user) {
        if (user == null) {
            return Map.of("error", "Unauthorized");
        }
        return Map.of(
            "stats", portfolioService.getPortfolioSummary(user.getId()),
            "items", portfolioService.getPortfolioItems(user.getId()),
            "balance", walletService.getTotalValuationInUsd(user.getId())
        );
    }
}
