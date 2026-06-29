package com.capitall.controller;

import com.capitall.model.User;
import com.capitall.model.Wallet;
import com.capitall.service.ExchangeRateService;
import com.capitall.service.WalletService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Controller
public class ExchangeController {

    private final WalletService walletService;
    private final ExchangeRateService exchangeRateService;

    public ExchangeController(WalletService walletService, ExchangeRateService exchangeRateService) {
        this.walletService = walletService;
        this.exchangeRateService = exchangeRateService;
    }

    @GetMapping("/exchange")
    public String viewExchange(Model model, @AuthenticationPrincipal User user) {
        if (user == null) {
            return "redirect:/login";
        }

        Wallet wallet = walletService.getOrCreate(user.getId());
        model.addAttribute("wallet", wallet);
        model.addAttribute("rates", exchangeRateService.getExchangeRatesList());
        model.addAttribute("traderName", user.getUsername());

        return "exchange";
    }

    @PostMapping("/exchange/convert")
    public String convertCurrency(@AuthenticationPrincipal User user,
                                  @RequestParam String from,
                                  @RequestParam String to,
                                  @RequestParam BigDecimal amount,
                                  RedirectAttributes redirectAttributes) {
        if (user == null) {
            return "redirect:/login";
        }

        try {
            walletService.exchangeCurrency(user.getId(), from, to, amount);
            redirectAttributes.addFlashAttribute("successMessage", 
                    String.format("Pomyślnie wymieniono %s %s na drugą walutę.", amount, from.toUpperCase()));
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Błąd wymiany: " + e.getMessage());
        }

        return "redirect:/exchange";
    }

    @PostMapping("/exchange/deposit")
    public String deposit(@AuthenticationPrincipal User user,
                          @RequestParam String currency,
                          @RequestParam BigDecimal amount,
                          RedirectAttributes redirectAttributes) {
        if (user == null) {
            return "redirect:/login";
        }

        try {
            walletService.deposit(user.getId(), currency, amount);
            redirectAttributes.addFlashAttribute("successMessage", 
                    String.format("Pomyślnie zasilono portfel kwotą %s %s.", amount, currency.toUpperCase()));
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Błąd zasilenia: " + e.getMessage());
        }

        return "redirect:/exchange";
    }

    @PostMapping("/exchange/withdraw")
    public String withdraw(@AuthenticationPrincipal User user,
                           @RequestParam String currency,
                           @RequestParam BigDecimal amount,
                           RedirectAttributes redirectAttributes) {
        if (user == null) {
            return "redirect:/login";
        }

        try {
            walletService.withdraw(user.getId(), currency, amount);
            redirectAttributes.addFlashAttribute("successMessage", 
                    String.format("Pomyślnie wypłacono kwotę %s %s z portfela.", amount, currency.toUpperCase()));
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Błąd wypłaty: " + e.getMessage());
        }

        return "redirect:/exchange";
    }

    @GetMapping("/exchange/rates")
    @org.springframework.web.bind.annotation.ResponseBody
    public Map<String, Object> getLiveRates(@AuthenticationPrincipal User user) {
        if (user == null) {
            return Map.of("error", "Unauthorized");
        }
        return Map.of(
            "rates", exchangeRateService.getExchangeRatesList(),
            "wallet", walletService.getOrCreate(user.getId())
        );
    }
}
