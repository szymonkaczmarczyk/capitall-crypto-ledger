package com.capitall.controller;

import com.capitall.model.PriceAlert;
import com.capitall.model.PriceAlertLog;
import com.capitall.model.User;
import com.capitall.service.PriceAlertService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
public class PriceAlertController {

    private final PriceAlertService alertService;

    public PriceAlertController(PriceAlertService alertService) {
        this.alertService = alertService;
    }

    // ── GET /alerts ───────────────────────────────────────────────────────────

    @GetMapping("/alerts")
    public String viewAlerts(Model model, @AuthenticationPrincipal User user) {
        if (user == null) return "redirect:/login";

        List<PriceAlert> activeAlerts = alertService.getActiveAlerts(user.getId());
        long cryptoCount = activeAlerts.stream().filter(a -> "CRYPTO".equals(a.getAssetType())).count();
        long stockCount  = activeAlerts.stream().filter(a -> "STOCK".equals(a.getAssetType())).count();
        List<PriceAlertLog> logs = alertService.getLogsForUser(user.getId());

        model.addAttribute("alerts", activeAlerts);
        model.addAttribute("cryptoCount", cryptoCount);
        model.addAttribute("stockCount", stockCount);
        model.addAttribute("alertLogs", logs);
        model.addAttribute("traderName", user.getUsername());
        return "alerts";
    }

    // ── POST /alerts/create ───────────────────────────────────────────────────

    @PostMapping("/alerts/create")
    public String createAlert(@AuthenticationPrincipal User user,
                              @RequestParam String assetType,
                              @RequestParam String symbol,
                              @RequestParam String conditionType,
                              @RequestParam BigDecimal targetPrice,
                              @RequestParam(defaultValue = "false") boolean recurring,
                              RedirectAttributes redirectAttributes) {
        if (user == null) return "redirect:/login";
        try {
            alertService.createAlert(user.getId(), assetType, symbol, conditionType, targetPrice, recurring);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Pomyślnie utworzono alert dla " + symbol.toUpperCase());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Błąd tworzenia alertu: " + e.getMessage());
        }
        return "redirect:/alerts";
    }

    // ── POST /alerts/update/{id} ──────────────────────────────────────────────

    @PostMapping("/alerts/update/{id}")
    public String updateAlert(@PathVariable UUID id,
                              @AuthenticationPrincipal User user,
                              @RequestParam String conditionType,
                              @RequestParam BigDecimal targetPrice,
                              @RequestParam(defaultValue = "false") boolean recurring,
                              RedirectAttributes redirectAttributes) {
        if (user == null) return "redirect:/login";
        try {
            alertService.updateAlert(id, user.getId(), conditionType, targetPrice, recurring);
            redirectAttributes.addFlashAttribute("successMessage", "Alert został zaktualizowany.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Błąd edycji alertu: " + e.getMessage());
        }
        return "redirect:/alerts";
    }

    // ── POST /alerts/delete/{id} ──────────────────────────────────────────────

    @PostMapping("/alerts/delete/{id}")
    public String deleteAlert(@PathVariable UUID id,
                              @AuthenticationPrincipal User user,
                              RedirectAttributes redirectAttributes) {
        if (user == null) return "redirect:/login";
        try {
            alertService.deleteAlert(id, user.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Usunięto alert.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Błąd usuwania alertu: " + e.getMessage());
        }
        return "redirect:/alerts";
    }

    // ── POST /api/alerts/check (AJAX heartbeat) ───────────────────────────────

    @PostMapping("/api/alerts/check")
    @ResponseBody
    public ResponseEntity<List<PriceAlert>> checkAlerts(@AuthenticationPrincipal User user) {
        if (user == null) return ResponseEntity.status(401).build();
        List<PriceAlert> triggered = alertService.checkAndTriggerAlertsForUser(user.getId());
        return ResponseEntity.ok(triggered);
    }
}
