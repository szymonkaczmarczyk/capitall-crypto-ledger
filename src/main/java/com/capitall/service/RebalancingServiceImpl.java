package com.capitall.service;

import com.capitall.dto.RebalanceResultDto;
import com.capitall.dto.RebalanceResultDto.AllocationDetail;
import com.capitall.dto.RebalanceResultDto.Recommendation;
import com.capitall.dto.RebalanceResultDto.TargetDetail;
import com.capitall.model.Holding;
import com.capitall.model.TargetAllocation;
import com.capitall.model.Wallet;
import com.capitall.repository.HoldingRepository;
import com.capitall.repository.TargetAllocationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class RebalancingServiceImpl implements RebalancingService {

    private final TargetAllocationRepository targetAllocationRepository;
    private final HoldingRepository holdingRepository;
    private final WalletService walletService;
    private final SecuritiesPriceService priceService;

    public RebalancingServiceImpl(TargetAllocationRepository targetAllocationRepository,
                                  HoldingRepository holdingRepository,
                                  WalletService walletService,
                                  SecuritiesPriceService priceService) {
        this.targetAllocationRepository = targetAllocationRepository;
        this.holdingRepository = holdingRepository;
        this.walletService = walletService;
        this.priceService = priceService;
    }

    @Override
    public List<TargetAllocation> getTargets(UUID userId) {
        return targetAllocationRepository.findByUserId(userId);
    }

    @Override
    @Transactional
    public void saveTargets(UUID userId, List<TargetAllocation> targets) {
        // Validate total percentage equals 100% (within roundoff, i.e. 1.0000)
        BigDecimal total = targets.stream()
                .map(TargetAllocation::getTargetPercentage)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (targets.isEmpty()) {
             targetAllocationRepository.deleteByUserId(userId);
             return;
        }

        // Allow slight roundoff error, e.g. 0.999 to 1.001
        if (total.compareTo(new BigDecimal("0.99")) < 0 || total.compareTo(new BigDecimal("1.01")) > 0) {
            throw new IllegalArgumentException("Suma alokacji docelowych musi wynosić 100% (aktualnie: " + total.multiply(BigDecimal.valueOf(100)) + "%)");
        }

        targetAllocationRepository.deleteByUserId(userId);
        for (TargetAllocation t : targets) {
            t.setUserId(userId);
            targetAllocationRepository.save(t);
        }
    }

    @Override
    public RebalanceResultDto calculateRebalancing(UUID userId) {
        List<Holding> holdings = holdingRepository.findByUserId(userId);
        Wallet wallet = walletService.getOrCreate(userId);
        List<TargetAllocation> targets = targetAllocationRepository.findByUserId(userId);

        BigDecimal usdBalance = wallet.getUsdBalance();

        // 1. Gather all current assets and their prices in USD
        Map<String, AssetInfo> currentAssets = new HashMap<>();

        // Add cash/stablecoin as "USD"
        currentAssets.put("USD", new AssetInfo(usdBalance, BigDecimal.ONE));

        for (Holding h : holdings) {
            BigDecimal price = priceService.getCurrentPrice(h.getSymbol());
            if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
                price = h.getAvgCost();
            }
            if (price == null) {
                price = BigDecimal.ZERO;
            }
            currentAssets.put(h.getSymbol().toUpperCase(), new AssetInfo(h.getAmount(), price));
        }

        // Add any target assets that are not currently held (with amount = 0)
        for (TargetAllocation t : targets) {
            String symbol = t.getSymbol().toUpperCase();
            if (!currentAssets.containsKey(symbol)) {
                BigDecimal price = priceService.getCurrentPrice(symbol);
                if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
                    price = BigDecimal.ZERO;
                }
                currentAssets.put(symbol, new AssetInfo(BigDecimal.ZERO, price));
            }
        }

        // 2. Calculate total portfolio value in USD
        BigDecimal totalValuation = BigDecimal.ZERO;
        for (AssetInfo info : currentAssets.values()) {
            totalValuation = totalValuation.add(info.getValue());
        }

        List<AllocationDetail> currentDetails = new ArrayList<>();
        List<TargetDetail> targetDetails = new ArrayList<>();
        List<Recommendation> recommendations = new ArrayList<>();

        if (totalValuation.compareTo(BigDecimal.ZERO) <= 0) {
            return new RebalanceResultDto(BigDecimal.ZERO, List.of(), List.of(), List.of());
        }

        // Map targets for easy access
        Map<String, BigDecimal> targetMap = targets.stream()
                .collect(Collectors.toMap(t -> t.getSymbol().toUpperCase(), TargetAllocation::getTargetPercentage));

        // 3. Compute detailed metrics
        for (Map.Entry<String, AssetInfo> entry : currentAssets.entrySet()) {
            String symbol = entry.getKey();
            AssetInfo info = entry.getValue();

            BigDecimal currentPct = info.getValue()
                    .multiply(BigDecimal.valueOf(100))
                    .divide(totalValuation, 4, RoundingMode.HALF_UP);

            currentDetails.add(new AllocationDetail(
                    symbol,
                    info.amount.setScale(6, RoundingMode.HALF_UP),
                    info.price.setScale(2, RoundingMode.HALF_UP),
                    info.getValue().setScale(2, RoundingMode.HALF_UP),
                    currentPct
            ));

            BigDecimal targetPct = targetMap.getOrDefault(symbol, BigDecimal.ZERO);
            BigDecimal targetValue = totalValuation.multiply(targetPct);
            BigDecimal diffValue = targetValue.subtract(info.getValue());

            targetDetails.add(new TargetDetail(
                    symbol,
                    targetPct.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP),
                    targetValue.setScale(2, RoundingMode.HALF_UP),
                    diffValue.setScale(2, RoundingMode.HALF_UP)
            ));

            // Generate trade suggestion (exclude cash "USD" from direct buy/sell orders)
            if (!"USD".equals(symbol) && diffValue.compareTo(BigDecimal.ZERO) != 0 && info.price.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal quantity = diffValue.divide(info.price, 8, RoundingMode.HALF_UP);
                String action = diffValue.compareTo(BigDecimal.ZERO) > 0 ? "BUY" : "SELL";

                recommendations.add(new Recommendation(
                        symbol,
                        action,
                        quantity.abs().setScale(6, RoundingMode.HALF_UP),
                        diffValue.abs().setScale(2, RoundingMode.HALF_UP)
                ));
            }
        }

        // Sort details alphabetically by symbol
        currentDetails.sort(Comparator.comparing(AllocationDetail::getSymbol));
        targetDetails.sort(Comparator.comparing(TargetDetail::getSymbol));
        recommendations.sort(Comparator.comparing(Recommendation::getSymbol));

        return new RebalanceResultDto(
                totalValuation.setScale(2, RoundingMode.HALF_UP),
                currentDetails,
                targetDetails,
                recommendations
        );
    }

    private static class AssetInfo {
        BigDecimal amount;
        BigDecimal price;

        AssetInfo(BigDecimal amount, BigDecimal price) {
            this.amount = amount;
            this.price = price;
        }

        BigDecimal getValue() {
            return amount.multiply(price);
        }
    }
}
