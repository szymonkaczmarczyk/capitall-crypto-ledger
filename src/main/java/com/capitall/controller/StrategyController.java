package com.capitall.controller;

import com.capitall.model.TradingStrategy;
import com.capitall.model.User;
import com.capitall.service.StrategyService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.UUID;

@Controller
@RequestMapping("/strategies")
public class StrategyController {

    private final StrategyService strategyService;

    public StrategyController(StrategyService strategyService) {
        this.strategyService = strategyService;
    }

    @GetMapping
    public String showStrategiesPage(Model model, @AuthenticationPrincipal User user) {
        if (user == null) return "redirect:/login";
        model.addAttribute("strategies", strategyService.getUserStrategies(user.getId()));
        model.addAttribute("triggerTypes", TradingStrategy.TriggerType.values());
        model.addAttribute("active", "strategies");
        return "strategies";
    }

    @PostMapping("/create")
    public String createStrategy(@AuthenticationPrincipal User user,
                                 @RequestParam String name,
                                 @RequestParam String symbol,
                                 @RequestParam String exchange,
                                 @RequestParam TradingStrategy.TriggerType triggerType,
                                 @RequestParam BigDecimal tradeAmountUsd,
                                 @RequestParam(required = false) BigDecimal dipPercentage,
                                 @RequestParam(required = false) BigDecimal takeProfitPercent,
                                 @RequestParam(required = false) BigDecimal stopLossPercent,
                                 @RequestParam(required = false) BigDecimal trailingStopPercent,
                                 RedirectAttributes redirectAttributes) {
        if (user == null) return "redirect:/login";

        TradingStrategy strategy = new TradingStrategy();
        strategy.setUserId(user.getId());
        strategy.setName(name);
        strategy.setSymbol(symbol.toUpperCase());
        strategy.setExchange(exchange);
        strategy.setTriggerType(triggerType);
        strategy.setTradeAmountUsd(tradeAmountUsd);

        if (dipPercentage != null) strategy.setDipPercentage(dipPercentage);
        if (takeProfitPercent != null) strategy.setTakeProfitPercent(takeProfitPercent);
        if (stopLossPercent != null) strategy.setStopLossPercent(stopLossPercent);
        if (trailingStopPercent != null) strategy.setTrailingStopPercent(trailingStopPercent);
        strategy.setActive(true);

        strategyService.saveStrategy(strategy);
        redirectAttributes.addFlashAttribute("success", "Nowy bot handlowy '" + name + "' został utworzony i aktywowany!");
        return "redirect:/strategies";
    }

    @PostMapping("/{id}/toggle")
    public String toggleStrategy(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        if (user != null) {
            strategyService.toggleStrategy(id, user.getId());
        }
        return "redirect:/strategies";
    }

    @PostMapping("/{id}/delete")
    public String deleteStrategy(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        if (user != null) {
            strategyService.deleteStrategy(id, user.getId());
        }
        return "redirect:/strategies";
    }
}
