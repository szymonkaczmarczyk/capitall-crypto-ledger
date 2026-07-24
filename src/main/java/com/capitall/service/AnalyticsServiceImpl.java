package com.capitall.service;

import com.capitall.dto.DashboardStatsResponse;
import com.capitall.dto.PnLPoint;
import com.capitall.exception.ResourceNotFoundException;
import com.capitall.model.Allocation;
import com.capitall.model.AllocationStatus;
import com.capitall.model.User;
import com.capitall.repository.AllocationRepository;
import com.capitall.repository.ExchangeAccountRepository;
import com.capitall.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class AnalyticsServiceImpl implements AnalyticsService {

    private final ExchangeAccountRepository exchangeAccountRepository;
    private final AllocationRepository allocationRepository;
    private final UserRepository userRepository;

    public AnalyticsServiceImpl(ExchangeAccountRepository exchangeAccountRepository,
            AllocationRepository allocationRepository,
            UserRepository userRepository) {
        this.exchangeAccountRepository = exchangeAccountRepository;
        this.allocationRepository = allocationRepository;
        this.userRepository = userRepository;
    }

    @Override
    public DashboardStatsResponse getDashboardStats(UUID userId) {
        List<Allocation> allocations = allocationRepository.findByUserId(userId);
        BigDecimal totalAum = allocations.stream()
                .filter(alloc -> alloc.getStatus() == AllocationStatus.ACTIVE)
                .map(alloc -> alloc.getExchangeAccount().getAllocatedCapital())
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> groupedCapital = new HashMap<>();
        BigDecimal totalCapital = BigDecimal.ZERO;
        for (Allocation alloc : allocations) {
            if (alloc.getStatus() == AllocationStatus.ACTIVE) {
                BigDecimal cap = alloc.getExchangeAccount().getAllocatedCapital();
                if (cap != null) {
                    String name = alloc.getExchangeAccount().getExchangeName();
                    groupedCapital.put(name, groupedCapital.getOrDefault(name, BigDecimal.ZERO).add(cap));
                    totalCapital = totalCapital.add(cap);
                }
            }
        }

        Map<String, Double> diversification = new HashMap<>();
        if (totalCapital.compareTo(BigDecimal.ZERO) > 0) {
            double totalDouble = totalCapital.doubleValue();
            for (Map.Entry<String, BigDecimal> entry : groupedCapital.entrySet()) {
                double pct = (entry.getValue().doubleValue() / totalDouble) * 100.0;
                pct = Math.round(pct * 100.0) / 100.0;
                diversification.put(entry.getKey(), pct);
            }
        }

        return new DashboardStatsResponse(totalAum, diversification);
    }

    @Override
    public List<PnLPoint> simulatePnL(UUID userId, int days) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        List<Allocation> allocations = allocationRepository.findByUserId(userId);
        BigDecimal initialCapital = allocations.stream()
                .filter(alloc -> alloc.getStatus() == AllocationStatus.ACTIVE)
                .map(alloc -> alloc.getExchangeAccount().getAllocatedCapital())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (initialCapital.compareTo(BigDecimal.ZERO) <= 0) {
            initialCapital = BigDecimal.valueOf(10000.00);
        }

        long seed = userId.getMostSignificantBits() ^ userId.getLeastSignificantBits();
        Random random = new Random(seed);

        List<PnLPoint> points = new ArrayList<>();
        LocalDateTime start = LocalDateTime.now().minusDays(days);
        BigDecimal currentCapital = initialCapital;

        points.add(new PnLPoint(start, initialCapital, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)));

        for (int i = 1; i <= days; i++) {
            LocalDateTime timestamp = start.plusDays(i);

            double drift = 0.08;
            double volatility = 1.5;

            double randomShock = random.nextGaussian() * volatility;
            double changePercent = drift + randomShock;

            if (random.nextDouble() < 0.05) {
                changePercent -= (2.0 + random.nextDouble() * 3.0);
            }

            BigDecimal multiplier = BigDecimal.valueOf(1.0 + (changePercent / 100.0));
            currentCapital = currentCapital.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);

            BigDecimal diff = currentCapital.subtract(initialCapital);
            BigDecimal pnlPct = diff.divide(initialCapital, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100.0))
                    .setScale(2, RoundingMode.HALF_UP);

            points.add(new PnLPoint(timestamp, currentCapital, pnlPct));
        }

        return points;
    }
}
