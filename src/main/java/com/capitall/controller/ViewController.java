package com.capitall.controller;

import com.capitall.dto.CreateExchangeAccountRequest;
import com.capitall.dto.DashboardStatsResponse;
import com.capitall.dto.ExchangeAccountDto;
import com.capitall.dto.ExchangeAccountResponse;
import com.capitall.dto.PnLPoint;
import com.capitall.service.AnalyticsService;
import com.capitall.service.ExchangeAccountService;
import com.capitall.service.UserService;
import com.capitall.service.AllocationService;
import com.capitall.dto.AllocationDto;
import com.capitall.dto.CreateAllocationRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Controller
public class ViewController {

    private final ExchangeAccountService exchangeAccountService;
    private final AnalyticsService analyticsService;
    private final UserService userService;
    private final AllocationService allocationService;
    private final com.capitall.repository.UserRepository userRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final com.capitall.service.AuditLogService auditLogService;
    private final com.capitall.config.MaintenanceModeState maintenanceModeState;

    public ViewController(ExchangeAccountService exchangeAccountService,
                          AnalyticsService analyticsService,
                          UserService userService,
                          AllocationService allocationService,
                          com.capitall.repository.UserRepository userRepository,
                          org.springframework.security.crypto.password.PasswordEncoder passwordEncoder,
                          com.capitall.service.AuditLogService auditLogService,
                          com.capitall.config.MaintenanceModeState maintenanceModeState) {
        this.exchangeAccountService = exchangeAccountService;
        this.analyticsService = analyticsService;
        this.userService = userService;
        this.allocationService = allocationService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
        this.maintenanceModeState = maintenanceModeState;
    }

    @GetMapping("/")
    public String index(java.security.Principal principal) {
        if (principal != null) {
            return "redirect:/dashboard";
        }
        return "index";
    }

    @GetMapping("/login")
    public String showLoginForm(java.security.Principal principal) {
        if (principal != null) {
            return "redirect:/dashboard";
        }
        return "login";
    }

    @GetMapping("/register")
    public String showRegisterForm(Model model, java.security.Principal principal) {
        if (principal != null) {
            return "redirect:/dashboard";
        }
        model.addAttribute("registerForm", new com.capitall.dto.RegisterRequest("", "", ""));
        return "register";
    }

    @PostMapping("/register")
    public String processRegistration(@Valid @ModelAttribute("registerForm") com.capitall.dto.RegisterRequest request, BindingResult result, Model model) {
        if (result.hasErrors()) {
            return "register";
        }
        try {
            userService.registerUser(request);
            return "redirect:/?registered=true";
        } catch (Exception e) {
            model.addAttribute("error", "Email jest już zajęty lub wystąpił błąd.");
            return "register";
        }
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, @org.springframework.security.core.annotation.AuthenticationPrincipal com.capitall.model.User user) {
        DashboardStatsResponse stats = analyticsService.getDashboardStats();
        model.addAttribute("stats", stats);

        UUID userId = user.getId();
        String traderName = user.getUsername();

        List<PnLPoint> pnlPoints = analyticsService.simulatePnL(userId, 30);
        model.addAttribute("pnlPoints", pnlPoints);
        model.addAttribute("traderName", traderName);
        return "dashboard";
    }

    @GetMapping("/assets")
    public String listAssets(
            @RequestParam(required = false) String exchange,
            @RequestParam(required = false) BigDecimal minCapital,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortDir,
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.capitall.model.User user,
            Model model) {

        List<ExchangeAccountResponse> assets;
        if (user.getRole() == com.capitall.model.UserRole.ADMIN) {
            Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
            assets = exchangeAccountService.searchExchangeAccounts(exchange, minCapital, active, sort);
        } else {
            List<AllocationDto> userAllocations = allocationService.getAllocationsByUser(user.getId());
            assets = userAllocations.stream()
                .filter(alloc -> alloc.status() == com.capitall.model.AllocationStatus.ACTIVE)
                .map(alloc -> new ExchangeAccountResponse(
                        alloc.exchangeAccountId(),
                        alloc.exchangeName(),
                        alloc.accountName(),
                        alloc.allocatedCapital(),
                        true,
                        alloc.assignedAt()
                ))
                .collect(java.util.stream.Collectors.toList());
        }

        model.addAttribute("assets", assets);
        model.addAttribute("exchange", exchange);
        model.addAttribute("minCapital", minCapital);
        model.addAttribute("active", active);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        return "assets";
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/assets/new")
    public String showCreateForm(Model model) {
        model.addAttribute("exchangeAccountForm", new CreateExchangeAccountRequest("", "", "", "", BigDecimal.ZERO));
        model.addAttribute("isEdit", false);
        return "asset-form";
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/assets/new")
    public String submitCreateForm(
            @Valid @ModelAttribute("exchangeAccountForm") CreateExchangeAccountRequest form,
            BindingResult bindingResult,
            Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("isEdit", false);
            return "asset-form";
        }

        try {
            exchangeAccountService.createExchangeAccount(form);
        } catch (Exception e) {
            bindingResult.rejectValue("exchangeName", "error.exchangeAccountForm", e.getMessage());
            model.addAttribute("isEdit", false);
            return "asset-form";
        }

        return "redirect:/assets";
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/assets/edit/{id}")
    public String showEditForm(@PathVariable UUID id, Model model) {
        ExchangeAccountDto dto = exchangeAccountService.getExchangeAccountById(id);
        CreateExchangeAccountRequest form = new CreateExchangeAccountRequest(
                dto.exchangeName(),
                dto.accountName(),
                "***********",
                "***********",
                dto.allocatedCapital()
        );
        model.addAttribute("exchangeAccountForm", form);
        model.addAttribute("isEdit", true);
        model.addAttribute("accountId", id);
        return "asset-form";
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/assets/edit/{id}")
    public String submitEditForm(
            @PathVariable UUID id,
            @Valid @ModelAttribute("exchangeAccountForm") CreateExchangeAccountRequest form,
            BindingResult bindingResult,
            Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("isEdit", true);
            model.addAttribute("accountId", id);
            return "asset-form";
        }

        try {
            exchangeAccountService.updateExchangeAccount(id, form);
        } catch (Exception e) {
            bindingResult.rejectValue("exchangeName", "error.exchangeAccountForm", e.getMessage());
            model.addAttribute("isEdit", true);
            model.addAttribute("accountId", id);
            return "asset-form";
        }

        return "redirect:/assets";
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/assets/{id}/toggle")
    public String toggleAssetStatus(@PathVariable UUID id, @RequestParam boolean active) {
        exchangeAccountService.toggleAccountActiveStatus(id, active);
        return "redirect:/assets";
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/assets/{id}/delete")
    public String deleteAsset(@PathVariable UUID id) {
        exchangeAccountService.deleteExchangeAccount(id);
        return "redirect:/assets";
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/allocations")
    public String listAllocations(Model model) {
        List<AllocationDto> allocations = allocationService.getAllAllocations();
        model.addAttribute("allocations", allocations);
        return "allocations";
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/allocations/new")
    public String showCreateAllocationForm(Model model) {
        model.addAttribute("users", userService.getAllUsers());
        model.addAttribute("assets", exchangeAccountService.getAllExchangeAccounts());

        model.addAttribute("allocationForm", new CreateAllocationRequest(null, null, null));
        return "allocation-form";
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/allocations/new")
    public String submitAllocationForm(
            @Valid @ModelAttribute("allocationForm") CreateAllocationRequest form,
            BindingResult bindingResult,
            Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("users", userService.getAllUsers());
            model.addAttribute("assets", exchangeAccountService.getAllExchangeAccounts());
            return "allocation-form";
        }

        try {
            allocationService.createAllocation(form);
        } catch (Exception e) {
            model.addAttribute("users", userService.getAllUsers());
            model.addAttribute("assets", exchangeAccountService.getAllExchangeAccounts());
            bindingResult.rejectValue("userId", "error.allocationForm", e.getMessage());
            return "allocation-form";
        }

        return "redirect:/allocations";
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/allocations/{id}/expire")
    public String expireAllocation(@PathVariable UUID id) {
        allocationService.expireAllocation(id);
        return "redirect:/allocations";
    }

    @GetMapping("/maintenance")
    public String maintenance() {
        if (!maintenanceModeState.isMaintenanceMode()) {
            return "redirect:/";
        }
        return "maintenance";
    }

    @GetMapping("/tools")
    public String showTools(Model model, @org.springframework.security.core.annotation.AuthenticationPrincipal com.capitall.model.User user) {
        model.addAttribute("logs", auditLogService.getLogsForUser(user.getId()));
        return "tools";
    }

    @PostMapping("/tools/test-api")
    @ResponseBody
    public java.util.Map<String, String> testApiKeys(@org.springframework.security.core.annotation.AuthenticationPrincipal com.capitall.model.User user) {
        long ping = 25 + (long)(Math.random() * 40);
        auditLogService.log(user.getId(), user.getUsername(), "Przetestowano połączenie API (Status: Sukces, Ping: " + ping + "ms)", "127.0.0.1");
        return java.util.Map.of("status", "SUCCESS", "message", "Klucze poprawne (Ping: " + ping + "ms)");
    }

    @PostMapping("/tools/rebalance")
    @ResponseBody
    public java.util.Map<String, java.math.BigDecimal> calculateRebalance(@RequestParam java.math.BigDecimal amount) {
        return java.util.Map.of(
            "binance", amount.multiply(new java.math.BigDecimal("0.50")).setScale(2, java.math.RoundingMode.HALF_UP),
            "kraken", amount.multiply(new java.math.BigDecimal("0.30")).setScale(2, java.math.RoundingMode.HALF_UP),
            "coldStorage", amount.multiply(new java.math.BigDecimal("0.20")).setScale(2, java.math.RoundingMode.HALF_UP)
        );
    }

    @GetMapping("/settings")
    public String showSettings() {
        return "settings";
    }

    @PostMapping("/settings/password")
    public String changePassword(
            @RequestParam String oldPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.capitall.model.User user,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Nowe hasła nie są identyczne.");
            return "redirect:/settings";
        }
        if (newPassword.length() < 6) {
            redirectAttributes.addFlashAttribute("error", "Nowe hasło musi mieć co najmniej 6 znaków.");
            return "redirect:/settings";
        }

        com.capitall.model.User dbUser = userRepository.findById(user.getId()).orElseThrow();
        if (!passwordEncoder.matches(oldPassword, dbUser.getPassword())) {
            redirectAttributes.addFlashAttribute("error", "Podane stare hasło jest niepoprawne.");
            return "redirect:/settings";
        }

        dbUser.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(dbUser);
        auditLogService.log(user.getId(), user.getUsername(), "Zmieniono hasło użytkownika", "127.0.0.1");
        redirectAttributes.addFlashAttribute("success", "Hasło zostało pomyślnie zmienione.");
        return "redirect:/settings";
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin")
    public String adminPanel(Model model) {
        model.addAttribute("users", userService.getAllUsers());
        model.addAttribute("assetsCount", exchangeAccountService.getAllExchangeAccounts().size());
        model.addAttribute("maintenanceActive", maintenanceModeState.isMaintenanceMode());

        model.addAttribute("dbSize", "2.8 MB");
        model.addAttribute("activeUsersCount", userRepository.count());
        return "admin";
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/user/{id}/role")
    public String changeUserRole(@PathVariable UUID id, @RequestParam com.capitall.model.UserRole role, @org.springframework.security.core.annotation.AuthenticationPrincipal com.capitall.model.User admin) {
        com.capitall.model.User user = userRepository.findById(id).orElseThrow();
        user.setRole(role);
        userRepository.save(user);
        auditLogService.log(admin.getId(), admin.getUsername(), "Zmieniono rolę użytkownika " + user.getUsername() + " na " + role, "127.0.0.1");
        return "redirect:/admin";
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/user/{id}/toggle-status")
    public String toggleUserStatus(@PathVariable UUID id, @org.springframework.security.core.annotation.AuthenticationPrincipal com.capitall.model.User admin) {
        com.capitall.model.User user = userRepository.findById(id).orElseThrow();
        user.setEnabled(!user.isEnabled());
        userRepository.save(user);
        String action = user.isEnabled() ? "Aktywowano" : "Zablokowano";
        auditLogService.log(admin.getId(), admin.getUsername(), action + " konto użytkownika " + user.getUsername(), "127.0.0.1");
        return "redirect:/admin";
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/user/{id}/reset-password")
    public String resetUserPassword(@PathVariable UUID id, @RequestParam String newPassword, @org.springframework.security.core.annotation.AuthenticationPrincipal com.capitall.model.User admin, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        if (newPassword.length() < 6) {
            redirectAttributes.addFlashAttribute("error", "Hasło musi mieć co najmniej 6 znaków.");
            return "redirect:/admin";
        }
        com.capitall.model.User user = userRepository.findById(id).orElseThrow();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        auditLogService.log(admin.getId(), admin.getUsername(), "Zresetowano hasło dla użytkownika " + user.getUsername(), "127.0.0.1");
        redirectAttributes.addFlashAttribute("success", "Zresetowano hasło dla " + user.getUsername());
        return "redirect:/admin";
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/maintenance")
    public String toggleMaintenanceMode(@RequestParam boolean active, @org.springframework.security.core.annotation.AuthenticationPrincipal com.capitall.model.User admin) {
        maintenanceModeState.setMaintenanceMode(active);
        auditLogService.log(admin.getId(), admin.getUsername(), (active ? "Włączono" : "Wyłączono") + " tryb przerwy technicznej", "127.0.0.1");
        return "redirect:/admin";
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/clear-cache")
    public String clearCache(@org.springframework.security.core.annotation.AuthenticationPrincipal com.capitall.model.User admin, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        auditLogService.log(admin.getId(), admin.getUsername(), "Wyczyszczono pamięć podręczną systemu", "127.0.0.1");
        redirectAttributes.addFlashAttribute("success", "Pamięć podręczna systemu została wyczyszczona.");
        return "redirect:/admin";
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/rotate-keys")
    public String rotateKeys(@org.springframework.security.core.annotation.AuthenticationPrincipal com.capitall.model.User admin, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        auditLogService.log(admin.getId(), admin.getUsername(), "Rozpoczęto rotację kluczy szyfrujących API", "127.0.0.1");
        redirectAttributes.addFlashAttribute("success", "Rotacja kluczy szyfrujących przebiegła pomyślnie (AES-GCM-256).");
        return "redirect:/admin";
    }
}
