package com.capitall.service;

import com.capitall.model.Holding;
import com.capitall.model.Trade;
import com.capitall.model.Wallet;
import com.capitall.repository.HoldingRepository;
import com.capitall.repository.TradeRepository;
import com.capitall.repository.WalletRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
public class WalletService {

    public static final BigDecimal DEFAULT_BALANCE = new BigDecimal("1500.00");
    public static final BigDecimal FEE_RATE = new BigDecimal("0.001");

    private final WalletRepository walletRepository;
    private final HoldingRepository holdingRepository;
    private final TradeRepository tradeRepository;

    public WalletService(WalletRepository walletRepository, HoldingRepository holdingRepository, TradeRepository tradeRepository) {
        this.walletRepository = walletRepository;
        this.holdingRepository = holdingRepository;
        this.tradeRepository = tradeRepository;
    }

    @Transactional(readOnly = true)
    public List<Trade> getRecentTrades(UUID userId, int limit) {
        return tradeRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, Math.max(1, limit)));
    }

    @Transactional
    public Wallet getOrCreate(UUID userId) {
        return walletRepository.findByUserId(userId).orElseGet(() ->
                walletRepository.save(new Wallet(userId, DEFAULT_BALANCE))
        );
    }

    private Wallet lockWallet(UUID userId) {
        return walletRepository.findByUserIdForUpdate(userId).orElseGet(() ->
                walletRepository.save(new Wallet(userId, DEFAULT_BALANCE))
        );
    }

    @Transactional(readOnly = true)
    public List<Holding> getHoldings(UUID userId) {
        return holdingRepository.findByUserId(userId);
    }

    public record TradeResult(BigDecimal coinAmount, BigDecimal fee, BigDecimal cash, BigDecimal totalAmountAfter) {}

    @Transactional
    public TradeResult buy(UUID userId, String symbol, BigDecimal price, BigDecimal usdAmount) {
        if (usdAmount == null || usdAmount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Kwota musi być większa od zera.");
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Nieprawidłowa cena rynkowa.");

        Wallet wallet = lockWallet(userId);
        BigDecimal fee = usdAmount.multiply(FEE_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = usdAmount.add(fee);
        if (wallet.getUsdBalance().compareTo(total) < 0) {
            throw new IllegalStateException("Niewystarczające środki. Dostępne: $" + wallet.getUsdBalance().setScale(2, RoundingMode.HALF_UP));
        }

        BigDecimal coinAmount = usdAmount.divide(price, 8, RoundingMode.HALF_UP);
        wallet.setUsdBalance(wallet.getUsdBalance().subtract(total).setScale(2, RoundingMode.HALF_UP));
        walletRepository.save(wallet);

        Holding holding = holdingRepository.findByUserIdAndSymbol(userId, symbol)
                .orElseGet(() -> new Holding(userId, symbol));
        BigDecimal oldAmt = holding.getAmount();
        BigDecimal oldCostBasis = oldAmt.multiply(holding.getAvgCost());
        BigDecimal newAmt = oldAmt.add(coinAmount);
        BigDecimal newCostBasis = oldCostBasis.add(usdAmount);
        BigDecimal newAvg = newAmt.compareTo(BigDecimal.ZERO) > 0
                ? newCostBasis.divide(newAmt, 8, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        holding.setAmount(newAmt.setScale(8, RoundingMode.HALF_UP));
        holding.setAvgCost(newAvg);
        holdingRepository.save(holding);

        Trade trade = new Trade();
        trade.setUserId(userId);
        trade.setSymbol(symbol);
        trade.setSide(Trade.Side.BUY);
        trade.setCoinAmount(coinAmount);
        trade.setPrice(price.setScale(8, RoundingMode.HALF_UP));
        trade.setFee(fee);
        trade.setGross(usdAmount.setScale(8, RoundingMode.HALF_UP));
        trade.setRealizedPnl(BigDecimal.ZERO);
        tradeRepository.save(trade);

        return new TradeResult(coinAmount, fee, wallet.getUsdBalance(), holding.getAmount());
    }

    @Transactional
    public TradeResult sell(UUID userId, String symbol, BigDecimal price, BigDecimal coinAmount) {
        if (coinAmount == null || coinAmount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Ilość musi być większa od zera.");
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Nieprawidłowa cena rynkowa.");

        Holding holding = holdingRepository.findByUserIdAndSymbol(userId, symbol)
                .orElseThrow(() -> new IllegalStateException("Brak posiadanych jednostek " + symbol + "."));
        if (holding.getAmount().compareTo(coinAmount) < 0) {
            throw new IllegalStateException("Nie posiadasz tyle " + symbol + ". Dostępne: " + holding.getAmount().stripTrailingZeros().toPlainString());
        }

        BigDecimal gross = coinAmount.multiply(price);
        BigDecimal fee = gross.multiply(FEE_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal net = gross.subtract(fee).setScale(2, RoundingMode.HALF_UP);
        BigDecimal avgCost = holding.getAvgCost();
        BigDecimal realizedPnl = price.subtract(avgCost).multiply(coinAmount).setScale(8, RoundingMode.HALF_UP);

        Wallet wallet = lockWallet(userId);
        wallet.setUsdBalance(wallet.getUsdBalance().add(net).setScale(2, RoundingMode.HALF_UP));
        walletRepository.save(wallet);

        BigDecimal remaining = holding.getAmount().subtract(coinAmount).setScale(8, RoundingMode.HALF_UP);
        if (remaining.compareTo(new BigDecimal("0.00000001")) <= 0) {
            holdingRepository.delete(holding);
            remaining = BigDecimal.ZERO;
        } else {
            holding.setAmount(remaining);
            holdingRepository.save(holding);
        }

        Trade trade = new Trade();
        trade.setUserId(userId);
        trade.setSymbol(symbol);
        trade.setSide(Trade.Side.SELL);
        trade.setCoinAmount(coinAmount);
        trade.setPrice(price.setScale(8, RoundingMode.HALF_UP));
        trade.setFee(fee);
        trade.setGross(gross.setScale(8, RoundingMode.HALF_UP));
        trade.setRealizedPnl(realizedPnl);
        tradeRepository.save(trade);

        return new TradeResult(coinAmount, fee, wallet.getUsdBalance(), remaining);
    }

    @Transactional
    public Wallet deposit(UUID userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Kwota doładowania musi być większa od zera.");
        }
        Wallet wallet = lockWallet(userId);
        wallet.setUsdBalance(wallet.getUsdBalance().add(amount).setScale(2, RoundingMode.HALF_UP));
        return walletRepository.save(wallet);
    }
}
