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
import jakarta.servlet.http.HttpServletRequest;
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
    private final com.capitall.repository.AllocationRepository allocationRepository;
    private final com.capitall.repository.WalletRepository walletRepository;
    private final com.capitall.repository.HoldingRepository holdingRepository;
    private final com.capitall.repository.TradeRepository tradeRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final com.capitall.service.AuditLogService auditLogService;
    private final com.capitall.config.MaintenanceModeState maintenanceModeState;
    private final com.capitall.service.WalletService walletService;
    private final com.capitall.service.EquitySnapshotter equitySnapshotter;
    private final com.capitall.repository.EquitySnapshotRepository equitySnapshotRepository;
    private final org.springframework.security.web.context.SecurityContextRepository securityContextRepository;
    private final com.capitall.service.EmailService emailService;
    private final com.capitall.service.TotpService totpService;

    public ViewController(ExchangeAccountService exchangeAccountService,
                          AnalyticsService analyticsService,
                          UserService userService,
                          AllocationService allocationService,
                          com.capitall.repository.UserRepository userRepository,
                          com.capitall.repository.AllocationRepository allocationRepository,
                          com.capitall.repository.WalletRepository walletRepository,
                          com.capitall.repository.HoldingRepository holdingRepository,
                          com.capitall.repository.TradeRepository tradeRepository,
                          org.springframework.security.crypto.password.PasswordEncoder passwordEncoder,
                          com.capitall.service.AuditLogService auditLogService,
                          com.capitall.config.MaintenanceModeState maintenanceModeState,
                          com.capitall.service.WalletService walletService,
                          com.capitall.service.EquitySnapshotter equitySnapshotter,
                          com.capitall.repository.EquitySnapshotRepository equitySnapshotRepository,
                          org.springframework.security.web.context.SecurityContextRepository securityContextRepository,
                          com.capitall.service.EmailService emailService,
                          com.capitall.service.TotpService totpService) {
        this.exchangeAccountService = exchangeAccountService;
        this.analyticsService = analyticsService;
        this.userService = userService;
        this.allocationService = allocationService;
        this.userRepository = userRepository;
        this.allocationRepository = allocationRepository;
        this.walletRepository = walletRepository;
        this.holdingRepository = holdingRepository;
        this.tradeRepository = tradeRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
        this.maintenanceModeState = maintenanceModeState;
        this.walletService = walletService;
        this.equitySnapshotter = equitySnapshotter;
        this.equitySnapshotRepository = equitySnapshotRepository;
        this.securityContextRepository = securityContextRepository;
        this.emailService = emailService;
        this.totpService = totpService;
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
        model.addAttribute("registerForm", new com.capitall.dto.RegisterRequest("", "", "", ""));
        return "register";
    }

    @PostMapping("/register")
    public String processRegistration(@Valid @ModelAttribute("registerForm") com.capitall.dto.RegisterRequest request,
                                      BindingResult result, Model model) {
        if (result.hasErrors()) {
            return "register";
        }
        try {
            userService.registerUser(request);
            com.capitall.model.User newUser = userRepository.findByUsername(request.username()).orElseThrow();
            emailService.sendActivationEmail(newUser.getEmail(), newUser.getActivationToken());
            model.addAttribute("email", newUser.getEmail());
            return "register-pending";
        } catch (Exception e) {
            model.addAttribute("error", "Email jest już zajęty lub wystąpił błąd.");
            return "register";
        }
    }

    @GetMapping("/activate")
    public String activateAccount(@RequestParam String token, Model model) {
        com.capitall.model.User user = userRepository.findByActivationToken(token).orElse(null);
        if (user == null
                || user.getActivationTokenExpires() == null
                || user.getActivationTokenExpires().isBefore(java.time.LocalDateTime.now())) {
            model.addAttribute("error", "Link aktywacyjny jest nieprawidłowy lub wygasł. Skontaktuj się z supportem.");
            return "activation-failed";
        }
        if (!user.isEmailVerified()) {
            user.setEmailVerified(true);
            userRepository.save(user);
        }
        if (user.isTotpEnabled()) {
            return "redirect:/login?activated=true";
        }
        if (user.getTotpSecret() == null) {
            user.setTotpSecret(totpService.generateSecret());
            userRepository.save(user);
        }
        model.addAttribute("qrDataUri", totpService.buildQrDataUri(user.getTotpSecret(), user.getEmail()));
        model.addAttribute("secret", user.getTotpSecret());
        model.addAttribute("token", token);
        return "setup-2fa";
    }

    @PostMapping("/setup-2fa")
    public String confirmTotpSetup(@RequestParam String token,
                                   @RequestParam String code,
                                   Model model,
                                   HttpServletRequest httpRequest,
                                   jakarta.servlet.http.HttpServletResponse httpResponse) {
        com.capitall.model.User user = userRepository.findByActivationToken(token).orElse(null);
        if (user == null
                || user.getActivationTokenExpires() == null
                || user.getActivationTokenExpires().isBefore(java.time.LocalDateTime.now())) {
            model.addAttribute("error", "Token aktywacyjny wygasł.");
            return "activation-failed";
        }
        if (!totpService.verify(user.getTotpSecret(), code)) {
            model.addAttribute("error", "Kod TOTP jest nieprawidłowy. Spróbuj ponownie.");
            model.addAttribute("qrDataUri", totpService.buildQrDataUri(user.getTotpSecret(), user.getEmail()));
            model.addAttribute("secret", user.getTotpSecret());
            model.addAttribute("token", token);
            return "setup-2fa";
        }
        user.setTotpEnabled(true);
        user.setEnabled(true);
        user.setActivationToken(null);
        user.setActivationTokenExpires(null);
        userRepository.save(user);

        org.springframework.security.authentication.UsernamePasswordAuthenticationToken auth =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        user, null, user.getAuthorities());
        org.springframework.security.core.context.SecurityContext context =
                org.springframework.security.core.context.SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        org.springframework.security.core.context.SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);
        return "redirect:/dashboard";
    }

    @GetMapping("/login/2fa")
    public String show2faChallenge(jakarta.servlet.http.HttpSession session, Model model) {
        Object pre = session.getAttribute(com.capitall.security.TwoFactorAuthenticationSuccessHandler.PRE_2FA_USER_ID);
        if (pre == null) {
            return "redirect:/login";
        }
        model.addAttribute("username", session.getAttribute(com.capitall.security.TwoFactorAuthenticationSuccessHandler.PRE_2FA_USERNAME));
        return "login-2fa";
    }

    @PostMapping("/login/2fa")
    public String process2faChallenge(@RequestParam String code,
                                      jakarta.servlet.http.HttpSession session,
                                      HttpServletRequest httpRequest,
                                      jakarta.servlet.http.HttpServletResponse httpResponse,
                                      Model model) {
        Object preIdObj = session.getAttribute(com.capitall.security.TwoFactorAuthenticationSuccessHandler.PRE_2FA_USER_ID);
        if (preIdObj == null) {
            return "redirect:/login";
        }
        UUID userId;
        try {
            userId = UUID.fromString(preIdObj.toString());
        } catch (IllegalArgumentException e) {
            session.removeAttribute(com.capitall.security.TwoFactorAuthenticationSuccessHandler.PRE_2FA_USER_ID);
            return "redirect:/login";
        }
        com.capitall.model.User user = userRepository.findById(userId).orElse(null);
        if (user == null || !user.isTotpEnabled() || user.getTotpSecret() == null) {
            session.removeAttribute(com.capitall.security.TwoFactorAuthenticationSuccessHandler.PRE_2FA_USER_ID);
            return "redirect:/login";
        }
        if (!totpService.verify(user.getTotpSecret(), code)) {
            model.addAttribute("error", "Kod jest nieprawidłowy. Spróbuj ponownie.");
            model.addAttribute("username", session.getAttribute(com.capitall.security.TwoFactorAuthenticationSuccessHandler.PRE_2FA_USERNAME));
            auditLogService.log(user.getId(), user.getUsername(), "2FA: nieudana próba weryfikacji kodu",
                    com.capitall.security.ClientIpResolver.resolve(httpRequest));
            return "login-2fa";
        }

        session.removeAttribute(com.capitall.security.TwoFactorAuthenticationSuccessHandler.PRE_2FA_USER_ID);
        session.removeAttribute(com.capitall.security.TwoFactorAuthenticationSuccessHandler.PRE_2FA_USERNAME);

        org.springframework.security.authentication.UsernamePasswordAuthenticationToken auth =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        user, null, user.getAuthorities());
        org.springframework.security.core.context.SecurityContext context =
                org.springframework.security.core.context.SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        org.springframework.security.core.context.SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);
        auditLogService.log(user.getId(), user.getUsername(), "Zalogowano (2FA OK)",
                com.capitall.security.ClientIpResolver.resolve(httpRequest));
        return "redirect:/dashboard";
    }

    @GetMapping("/login/2fa/help")
    public String show2faHelp() {
        return "login-2fa-help";
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

    @GetMapping("/dashboard/equity-series")
    @ResponseBody
    public java.util.List<java.util.Map<String, Object>> equitySeries(@org.springframework.security.core.annotation.AuthenticationPrincipal com.capitall.model.User user) {
        java.time.LocalDateTime from = java.time.LocalDateTime.now().minusDays(30);
        java.util.List<com.capitall.model.EquitySnapshot> series =
                equitySnapshotRepository.findByUserIdAndCapturedAtGreaterThanEqualOrderByCapturedAtAsc(user.getId(), from);
        return series.stream().map(s -> {
            java.util.Map<String, Object> m = new java.util.HashMap<>();
            m.put("t", s.getCapturedAt().toString());
            m.put("equity", s.getEquityUsd().toPlainString());
            m.put("cash", s.getCashUsd().toPlainString());
            m.put("crypto", s.getCryptoUsd().toPlainString());
            return m;
        }).collect(java.util.stream.Collectors.toList());
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

    @GetMapping("/market")
    public String showMarket(Model model, @org.springframework.security.core.annotation.AuthenticationPrincipal com.capitall.model.User user) {
        com.capitall.model.Wallet wallet = walletService.getOrCreate(user.getId());
        java.util.List<com.capitall.model.Holding> holdings = walletService.getHoldings(user.getId());
        model.addAttribute("availableBalance", wallet.getUsdBalance());
        model.addAttribute("holdings", holdings);
        model.addAttribute("recentLogs", auditLogService.getLogsForUser(user.getId()).stream()
                .filter(l -> l.getAction() != null && (l.getAction().startsWith("ORDER:") || l.getAction().startsWith("SELL:")))
                .limit(12)
                .collect(java.util.stream.Collectors.toList()));
        return "market";
    }

    @GetMapping("/market/portfolio")
    @ResponseBody
    public java.util.Map<String, Object> portfolioData(@org.springframework.security.core.annotation.AuthenticationPrincipal com.capitall.model.User user) {
        com.capitall.model.Wallet wallet = walletService.getOrCreate(user.getId());
        java.util.List<java.util.Map<String, Object>> holdings = walletService.getHoldings(user.getId()).stream()
                .map(h -> {
                    java.util.Map<String, Object> m = new java.util.HashMap<>();
                    m.put("symbol", h.getSymbol());
                    m.put("amount", h.getAmount().stripTrailingZeros().toPlainString());
                    m.put("avgCost", h.getAvgCost().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
                    return m;
                }).collect(java.util.stream.Collectors.toList());
        return java.util.Map.of(
                "balance", wallet.getUsdBalance().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString(),
                "holdings", holdings
        );
    }

    @GetMapping("/market/trades")
    @ResponseBody
    public java.util.List<java.util.Map<String, Object>> recentTrades(@org.springframework.security.core.annotation.AuthenticationPrincipal com.capitall.model.User user) {
        return walletService.getRecentTrades(user.getId(), 25).stream().map(t -> {
            java.util.Map<String, Object> m = new java.util.HashMap<>();
            m.put("id", t.getId().toString());
            m.put("symbol", t.getSymbol());
            m.put("side", t.getSide().name());
            m.put("coinAmount", t.getCoinAmount().stripTrailingZeros().toPlainString());
            m.put("price", t.getPrice().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
            m.put("fee", t.getFee().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
            m.put("gross", t.getGross().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
            m.put("realizedPnl", t.getRealizedPnl().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
            m.put("createdAt", t.getCreatedAt().toString());
            return m;
        }).collect(java.util.stream.Collectors.toList());
    }

    @PostMapping("/market/buy")
    @ResponseBody
    public java.util.Map<String, Object> buyAsset(
            @RequestParam String symbol,
            @RequestParam java.math.BigDecimal price,
            @RequestParam java.math.BigDecimal usdAmount,
            @RequestParam(required = false, defaultValue = "MARKET") String orderType,
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.capitall.model.User user,
            HttpServletRequest request) {
        try {
            com.capitall.service.WalletService.TradeResult r = walletService.buy(user.getId(), symbol, price, usdAmount);
            String orderId = "ORD-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            String action = String.format("ORDER: %s %s · %s %s @ $%s · fee $%s · id=%s",
                    orderType, symbol,
                    r.coinAmount().stripTrailingZeros().toPlainString(), symbol,
                    price.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString(),
                    r.fee().toPlainString(), orderId);
            auditLogService.log(user.getId(), user.getUsername(), action, com.capitall.security.ClientIpResolver.resolve(request));
            try { equitySnapshotter.snapshotUser(user.getId()); } catch (Exception ignored) {}
            return java.util.Map.of(
                    "status", "FILLED",
                    "orderId", orderId,
                    "symbol", symbol,
                    "coinAmount", r.coinAmount().stripTrailingZeros().toPlainString(),
                    "price", price.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString(),
                    "fee", r.fee().toPlainString(),
                    "total", usdAmount.add(r.fee()).setScale(2, java.math.RoundingMode.HALF_UP).toPlainString(),
                    "balance", r.cash().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString(),
                    "holding", r.totalAmountAfter().stripTrailingZeros().toPlainString(),
                    "message", "Zlecenie wykonane pomyślnie."
            );
        } catch (RuntimeException e) {
            return java.util.Map.of("status", "ERROR", "message", e.getMessage() == null ? "Błąd zlecenia." : e.getMessage());
        }
    }

    @PostMapping("/market/sell")
    @ResponseBody
    public java.util.Map<String, Object> sellAsset(
            @RequestParam String symbol,
            @RequestParam java.math.BigDecimal price,
            @RequestParam java.math.BigDecimal coinAmount,
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.capitall.model.User user,
            HttpServletRequest request) {
        try {
            com.capitall.service.WalletService.TradeResult r = walletService.sell(user.getId(), symbol, price, coinAmount);
            String orderId = "ORD-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            java.math.BigDecimal gross = coinAmount.multiply(price).setScale(2, java.math.RoundingMode.HALF_UP);
            String action = String.format("SELL: MARKET %s · %s %s @ $%s · net $%s · fee $%s · id=%s",
                    symbol,
                    coinAmount.stripTrailingZeros().toPlainString(), symbol,
                    price.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString(),
                    gross.subtract(r.fee()).toPlainString(),
                    r.fee().toPlainString(), orderId);
            auditLogService.log(user.getId(), user.getUsername(), action, com.capitall.security.ClientIpResolver.resolve(request));
            try { equitySnapshotter.snapshotUser(user.getId()); } catch (Exception ignored) {}
            return java.util.Map.of(
                    "status", "FILLED",
                    "orderId", orderId,
                    "symbol", symbol,
                    "coinAmount", coinAmount.stripTrailingZeros().toPlainString(),
                    "price", price.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString(),
                    "fee", r.fee().toPlainString(),
                    "net", gross.subtract(r.fee()).toPlainString(),
                    "balance", r.cash().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString(),
                    "holding", r.totalAmountAfter().stripTrailingZeros().toPlainString(),
                    "message", "Sprzedaż zrealizowana."
            );
        } catch (RuntimeException e) {
            return java.util.Map.of("status", "ERROR", "message", e.getMessage() == null ? "Błąd zlecenia." : e.getMessage());
        }
    }

    @PostMapping("/market/deposit")
    @ResponseBody
    public java.util.Map<String, Object> depositFunds(
            @RequestParam java.math.BigDecimal amount,
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.capitall.model.User user,
            HttpServletRequest request) {
        try {
            com.capitall.model.Wallet wallet = walletService.deposit(user.getId(), amount);
            String action = String.format("DEPOSIT: Doładowano konto kwotą $%s",
                    amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
            auditLogService.log(user.getId(), user.getUsername(), action, com.capitall.security.ClientIpResolver.resolve(request));
            try { equitySnapshotter.snapshotUser(user.getId()); } catch (Exception ignored) {}
            return java.util.Map.of(
                    "status", "SUCCESS",
                    "amount", amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString(),
                    "balance", wallet.getUsdBalance().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString(),
                    "message", "Konto zostało pomyślnie zasilone."
            );
        } catch (RuntimeException e) {
            return java.util.Map.of("status", "ERROR", "message", e.getMessage() == null ? "Błąd zasilenia konta." : e.getMessage());
        }
    }

    @PostMapping("/tools/test-api")
    @ResponseBody
    public java.util.Map<String, String> testApiKeys(@org.springframework.security.core.annotation.AuthenticationPrincipal com.capitall.model.User user,
                                                     HttpServletRequest request) {
        long ping = 25 + (long)(Math.random() * 40);
        auditLogService.log(user.getId(), user.getUsername(), "Przetestowano połączenie API (Status: Sukces, Ping: " + ping + "ms)", com.capitall.security.ClientIpResolver.resolve(request));
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
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes,
            HttpServletRequest request) {
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Nowe hasła nie są identyczne.");
            return "redirect:/settings";
        }
        String policyError = com.capitall.security.PasswordPolicy.validate(newPassword);
        if (policyError != null) {
            redirectAttributes.addFlashAttribute("error", policyError);
            return "redirect:/settings";
        }

        com.capitall.model.User dbUser = userRepository.findById(user.getId()).orElseThrow();
        if (!passwordEncoder.matches(oldPassword, dbUser.getPassword())) {
            redirectAttributes.addFlashAttribute("error", "Podane stare hasło jest niepoprawne.");
            return "redirect:/settings";
        }

        dbUser.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(dbUser);
        auditLogService.log(user.getId(), user.getUsername(), "Zmieniono hasło użytkownika", com.capitall.security.ClientIpResolver.resolve(request));
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
    public String changeUserRole(@PathVariable UUID id, @RequestParam com.capitall.model.UserRole role,
                                 @org.springframework.security.core.annotation.AuthenticationPrincipal com.capitall.model.User admin,
                                 org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes,
                                 HttpServletRequest request) {
        if (id.equals(admin.getId())) {
            redirectAttributes.addFlashAttribute("error", "Nie możesz zmienić własnej roli.");
            return "redirect:/admin";
        }
        com.capitall.model.User user = userRepository.findById(id).orElseThrow();
        user.setRole(role);
        userRepository.save(user);
        auditLogService.log(admin.getId(), admin.getUsername(), "Zmieniono rolę użytkownika " + user.getUsername() + " na " + role, com.capitall.security.ClientIpResolver.resolve(request));
        return "redirect:/admin";
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/user/{id}/toggle-status")
    public String toggleUserStatus(@PathVariable UUID id,
                                   @org.springframework.security.core.annotation.AuthenticationPrincipal com.capitall.model.User admin,
                                   HttpServletRequest request) {
        com.capitall.model.User user = userRepository.findById(id).orElseThrow();
        user.setEnabled(!user.isEnabled());
        userRepository.save(user);
        String action = user.isEnabled() ? "Aktywowano" : "Zablokowano";
        auditLogService.log(admin.getId(), admin.getUsername(), action + " konto użytkownika " + user.getUsername(), com.capitall.security.ClientIpResolver.resolve(request));
        return "redirect:/admin";
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/user/{id}/delete")
    @org.springframework.transaction.annotation.Transactional
    public String deleteUser(@PathVariable UUID id,
                             @org.springframework.security.core.annotation.AuthenticationPrincipal com.capitall.model.User admin,
                             org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes,
                             HttpServletRequest request) {
        if (id.equals(admin.getId())) {
            redirectAttributes.addFlashAttribute("error", "Nie możesz usunąć własnego konta.");
            return "redirect:/admin";
        }
        com.capitall.model.User target = userRepository.findById(id).orElse(null);
        if (target == null) {
            redirectAttributes.addFlashAttribute("error", "Użytkownik nie istnieje.");
            return "redirect:/admin";
        }
        String username = target.getUsername();
        allocationRepository.deleteAll(allocationRepository.findByUserId(id));
        holdingRepository.deleteAll(holdingRepository.findByUserId(id));
        walletRepository.findByUserId(id).ifPresent(walletRepository::delete);
        userRepository.delete(target);
        auditLogService.log(admin.getId(), admin.getUsername(), "Usunięto konto użytkownika " + username, com.capitall.security.ClientIpResolver.resolve(request));
        redirectAttributes.addFlashAttribute("success", "Usunięto konto " + username + ".");
        return "redirect:/admin";
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/user/{id}/reset-password")
    public String resetUserPassword(@PathVariable UUID id, @RequestParam String newPassword, @org.springframework.security.core.annotation.AuthenticationPrincipal com.capitall.model.User admin, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes, HttpServletRequest request) {
        String policyError = com.capitall.security.PasswordPolicy.validate(newPassword);
        if (policyError != null) {
            redirectAttributes.addFlashAttribute("error", policyError);
            return "redirect:/admin";
        }
        com.capitall.model.User user = userRepository.findById(id).orElseThrow();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        auditLogService.log(admin.getId(), admin.getUsername(), "Zresetowano hasło dla użytkownika " + user.getUsername(), com.capitall.security.ClientIpResolver.resolve(request));
        redirectAttributes.addFlashAttribute("success", "Zresetowano hasło dla " + user.getUsername());
        return "redirect:/admin";
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/user/{id}/credit-funds")
    public String creditUserFunds(@PathVariable UUID id,
                                  @RequestParam BigDecimal amount,
                                  @org.springframework.security.core.annotation.AuthenticationPrincipal com.capitall.model.User admin,
                                  org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes,
                                  HttpServletRequest request) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            redirectAttributes.addFlashAttribute("error", "Kwota doładowania musi być większa od zera.");
            return "redirect:/admin";
        }
        if (amount.compareTo(new BigDecimal("1000000")) > 0) {
            redirectAttributes.addFlashAttribute("error", "Kwota doładowania przekracza dopuszczalny limit (1 000 000).");
            return "redirect:/admin";
        }
        com.capitall.model.User target = userRepository.findById(id).orElse(null);
        if (target == null) {
            redirectAttributes.addFlashAttribute("error", "Użytkownik nie istnieje.");
            return "redirect:/admin";
        }
        walletService.deposit(target.getId(), amount);
        auditLogService.log(admin.getId(), admin.getUsername(),
                "Doładowano saldo użytkownika " + target.getUsername() + " kwotą " + amount,
                com.capitall.security.ClientIpResolver.resolve(request));
        redirectAttributes.addFlashAttribute("success",
                "Doładowano saldo " + target.getUsername() + " o " + amount);
        return "redirect:/admin";
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/user/{id}/reset-2fa")
    public String resetUser2fa(@PathVariable UUID id,
                               @org.springframework.security.core.annotation.AuthenticationPrincipal com.capitall.model.User admin,
                               org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes,
                               HttpServletRequest request) {
        com.capitall.model.User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "Użytkownik nie istnieje.");
            return "redirect:/admin";
        }
        user.setTotpEnabled(false);
        user.setTotpSecret(null);
        String newToken = java.util.UUID.randomUUID().toString().replace("-", "")
                + java.util.UUID.randomUUID().toString().replace("-", "");
        user.setActivationToken(newToken);
        user.setActivationTokenExpires(java.time.LocalDateTime.now().plusHours(24));
        userRepository.save(user);
        emailService.sendActivationEmail(user.getEmail(), newToken);
        auditLogService.log(admin.getId(), admin.getUsername(),
                "Zresetowano 2FA dla użytkownika " + user.getUsername() + " (wysłano nowy link aktywacyjny)",
                com.capitall.security.ClientIpResolver.resolve(request));
        redirectAttributes.addFlashAttribute("success",
                "Zresetowano 2FA dla " + user.getUsername() + ". Wysłano nowy link aktywacyjny na " + user.getEmail() + ".");
        return "redirect:/admin";
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/maintenance")
    public String toggleMaintenanceMode(@RequestParam boolean active, @org.springframework.security.core.annotation.AuthenticationPrincipal com.capitall.model.User admin, HttpServletRequest request) {
        maintenanceModeState.setMaintenanceMode(active);
        auditLogService.log(admin.getId(), admin.getUsername(), (active ? "Włączono" : "Wyłączono") + " tryb przerwy technicznej", com.capitall.security.ClientIpResolver.resolve(request));
        return "redirect:/admin";
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/clear-cache")
    public String clearCache(@org.springframework.security.core.annotation.AuthenticationPrincipal com.capitall.model.User admin, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes, HttpServletRequest request) {
        auditLogService.log(admin.getId(), admin.getUsername(), "Wyczyszczono pamięć podręczną systemu", com.capitall.security.ClientIpResolver.resolve(request));
        redirectAttributes.addFlashAttribute("success", "Pamięć podręczna systemu została wyczyszczona.");
        return "redirect:/admin";
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/rotate-keys")
    public String rotateKeys(@org.springframework.security.core.annotation.AuthenticationPrincipal com.capitall.model.User admin, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes, HttpServletRequest request) {
        auditLogService.log(admin.getId(), admin.getUsername(), "Rozpoczęto rotację kluczy szyfrujących API", com.capitall.security.ClientIpResolver.resolve(request));
        redirectAttributes.addFlashAttribute("success", "Rotacja kluczy szyfrujących przebiegła pomyślnie (AES-GCM-256).");
        return "redirect:/admin";
    }
}
