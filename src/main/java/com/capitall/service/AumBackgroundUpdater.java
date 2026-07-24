package com.capitall.service;

import com.capitall.model.ExchangeAccount;
import com.capitall.repository.ExchangeAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Random;

@Component
public class AumBackgroundUpdater {

    private static final Logger log = LoggerFactory.getLogger(AumBackgroundUpdater.class);

    private final ExchangeAccountRepository exchangeAccountRepository;
    private final Random random = new Random();

    public AumBackgroundUpdater(ExchangeAccountRepository exchangeAccountRepository) {
        this.exchangeAccountRepository = exchangeAccountRepository;
    }

    @Scheduled(fixedRate = 15000)
    @Transactional
    public void fluctuateAum() {
        List<ExchangeAccount> accounts = exchangeAccountRepository.findAll();
        boolean updated = false;

        for (ExchangeAccount acc : accounts) {
            if (acc.isActive() && acc.getAllocatedCapital() != null && acc.getAllocatedCapital().compareTo(BigDecimal.ZERO) > 0) {
                double pctChange = (random.nextDouble() * 0.08 - 0.04) / 100.0;
                BigDecimal multiplier = BigDecimal.valueOf(1.0 + pctChange);
                BigDecimal newCapital = acc.getAllocatedCapital().multiply(multiplier).setScale(2, RoundingMode.HALF_UP);

                acc.setAllocatedCapital(newCapital);
                exchangeAccountRepository.save(acc);
                updated = true;
            }
        }

        if (updated) {
            log.debug("Zaktualizowano wirtualny AUM kont giełdowych w tle.");
        }
    }
}
