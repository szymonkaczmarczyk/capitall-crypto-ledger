package com.capitall.config;

import com.capitall.model.User;
import com.capitall.model.UserRole;
import com.capitall.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.capitall.repository.ExchangeAccountRepository exchangeAccountRepository;
    private final com.capitall.repository.AllocationRepository allocationRepository;
    private final com.capitall.service.ApiSecretsEncryptor apiSecretsEncryptor;

    public DataInitializer(UserRepository userRepository, 
                           PasswordEncoder passwordEncoder,
                           com.capitall.repository.ExchangeAccountRepository exchangeAccountRepository,
                           com.capitall.repository.AllocationRepository allocationRepository,
                           com.capitall.service.ApiSecretsEncryptor apiSecretsEncryptor) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.exchangeAccountRepository = exchangeAccountRepository;
        this.allocationRepository = allocationRepository;
        this.apiSecretsEncryptor = apiSecretsEncryptor;
    }

    @Override
    public void run(String... args) {

        User admin = createUserIfNotExists("admin", "admin@capitall.com", "admin1", UserRole.ADMIN);
        User jankowalski = createUserIfNotExists("jankowalski", "jan.kowalski@example.pl", "test123", UserRole.USER);
        User pnowak = createUserIfNotExists("pnowak", "piotr.nowak@example.pl", "test123", UserRole.USER);
        User mwisniewska = createUserIfNotExists("mwisniewska", "magda.wisniewska@example.pl", "test123", UserRole.USER);

        if (exchangeAccountRepository.count() == 0) {
            com.capitall.model.ExchangeAccount binanceMain = createExchangeAccount("BINANCE", "Binance (Main)", "12500.00");
            com.capitall.model.ExchangeAccount krakenTrading = createExchangeAccount("KRAKEN", "Kraken (Trading)", "4200.00");
            com.capitall.model.ExchangeAccount bybitBotA = createExchangeAccount("BYBIT", "Bybit (Bot A)", "8300.00");
            com.capitall.model.ExchangeAccount coinbaseCold = createExchangeAccount("COINBASE", "Coinbase (Cold)", "25000.00");
            com.capitall.model.ExchangeAccount binanceTest = createExchangeAccount("BINANCE", "Binance (Test)", "1500.00");

            createAllocation(jankowalski, binanceMain);
            createAllocation(jankowalski, krakenTrading);
            createAllocation(pnowak, bybitBotA);
            createAllocation(mwisniewska, coinbaseCold);
            createAllocation(admin, binanceTest);
        }
    }

    private User createUserIfNotExists(String username, String email, String password, UserRole role) {
        return userRepository.findByUsername(username).orElseGet(() -> 
            userRepository.save(User.builder()
                .id(UUID.randomUUID())
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(password))
                .role(role)
                .build())
        );
    }

    private com.capitall.model.ExchangeAccount createExchangeAccount(String exchangeName, String accountName, String capital) {
        return exchangeAccountRepository.save(com.capitall.model.ExchangeAccount.builder()
                .exchangeName(exchangeName)
                .accountName(accountName)
                .encryptedApiKey(apiSecretsEncryptor.encrypt("mock-api-key-" + UUID.randomUUID().toString().substring(0, 8)))
                .encryptedApiSecret(apiSecretsEncryptor.encrypt("mock-api-secret-" + UUID.randomUUID().toString().substring(0, 8)))
                .allocatedCapital(new java.math.BigDecimal(capital))
                .isActive(true)
                .build());
    }

    private void createAllocation(User user, com.capitall.model.ExchangeAccount account) {
        allocationRepository.save(com.capitall.model.Allocation.builder()
                .user(user)
                .exchangeAccount(account)
                .status(com.capitall.model.AllocationStatus.ACTIVE)
                .expiresAt(java.time.LocalDateTime.now().plusMonths(6))
                .build());
    }
}
