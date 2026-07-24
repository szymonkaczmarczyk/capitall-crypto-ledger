package com.capitall.service;

import com.capitall.dto.TaxReportDto;
import com.capitall.dto.TaxReportDto.TaxRow;
import com.capitall.model.SecurityTrade;
import com.capitall.model.Trade;
import com.capitall.repository.SecurityTradeRepository;
import com.capitall.repository.TradeRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TaxReportServiceImpl implements TaxReportService {

    private final TradeRepository tradeRepository;
    private final SecurityTradeRepository securityTradeRepository;
    private final ExchangeRateService exchangeRateService;

    public TaxReportServiceImpl(TradeRepository tradeRepository, SecurityTradeRepository securityTradeRepository, ExchangeRateService exchangeRateService) {
        this.tradeRepository = tradeRepository;
        this.securityTradeRepository = securityTradeRepository;
        this.exchangeRateService = exchangeRateService;
    }

    private static class UnifiedTrade {
        String symbol;
        String assetType; // "CRYPTO" or "SECURITY"
        boolean isBuy;
        BigDecimal amount;
        BigDecimal price;
        BigDecimal fee;
        String currency;
        LocalDateTime date;

        UnifiedTrade(String symbol, String assetType, boolean isBuy, BigDecimal amount, BigDecimal price, BigDecimal fee, String currency, LocalDateTime date) {
            this.symbol = symbol;
            this.assetType = assetType;
            this.isBuy = isBuy;
            this.amount = amount;
            this.price = price;
            this.fee = fee;
            this.currency = currency;
            this.date = date;
        }
    }

    private static class BuyPoolItem {
        LocalDateTime date;
        BigDecimal originalAmount;
        BigDecimal remainingAmount;
        BigDecimal price;
        BigDecimal unitFee; // Proportional fee per unit
        String currency;

        BuyPoolItem(LocalDateTime date, BigDecimal amount, BigDecimal price, BigDecimal totalFee, String currency) {
            this.date = date;
            this.originalAmount = amount;
            this.remainingAmount = amount;
            this.price = price;
            this.unitFee = amount.compareTo(BigDecimal.ZERO) > 0 ? totalFee.divide(amount, 8, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            this.currency = currency;
        }
    }

    @Override
    public TaxReportDto generateReport(UUID userId, int year, String method) {
        List<Trade> cryptoTrades = tradeRepository.findByUserIdOrderByCreatedAtAsc(userId);
        List<SecurityTrade> securityTrades = securityTradeRepository.findByUserIdOrderByCreatedAtAsc(userId);

        List<UnifiedTrade> allTrades = new ArrayList<>();
        for (Trade t : cryptoTrades) {
            allTrades.add(new UnifiedTrade(
                    t.getSymbol(),
                    "CRYPTO",
                    t.getSide() == Trade.Side.BUY,
                    t.getCoinAmount(),
                    t.getPrice(),
                    t.getFee(),
                    "USD", // Crypto trades default to USD
                    t.getCreatedAt()
            ));
        }
        for (SecurityTrade st : securityTrades) {
            allTrades.add(new UnifiedTrade(
                    st.getSymbol(),
                    "SECURITY",
                    st.getSide() == SecurityTrade.Side.BUY,
                    st.getShares(),
                    st.getPrice(),
                    st.getFee(),
                    st.getCurrency(),
                    st.getCreatedAt()
            ));
        }

        // Sort chronologically
        allTrades.sort(Comparator.comparing(t -> t.date));

        // Group by symbol and asset type to match buys/sells
        Map<String, List<UnifiedTrade>> grouped = allTrades.stream()
                .collect(Collectors.groupingBy(t -> t.assetType + ":" + t.symbol));

        List<TaxRow> allRows = new ArrayList<>();

        for (Map.Entry<String, List<UnifiedTrade>> entry : grouped.entrySet()) {
            String[] parts = entry.getKey().split(":");
            String assetType = parts[0];
            String symbol = parts[1];
            List<UnifiedTrade> trades = entry.getValue();

            // LinkedList to serve as buy queue (FIFO) or stack-like retrieval (LIFO)
            LinkedList<BuyPoolItem> buyPool = new LinkedList<>();

            for (UnifiedTrade trade : trades) {
                if (trade.isBuy) {
                    buyPool.add(new BuyPoolItem(trade.date, trade.amount, trade.price, trade.fee, trade.currency));
                } else {
                    // It's a sell. Match with buy pool.
                    BigDecimal remainingSellAmount = trade.amount;
                    BigDecimal sellPrice = trade.price;
                    BigDecimal totalSellFee = trade.fee;
                    BigDecimal unitSellFee = trade.amount.compareTo(BigDecimal.ZERO) > 0 
                            ? totalSellFee.divide(trade.amount, 8, RoundingMode.HALF_UP) : BigDecimal.ZERO;
                    String sellCurrency = trade.currency;

                    while (remainingSellAmount.compareTo(BigDecimal.ZERO) > 0 && !buyPool.isEmpty()) {
                        // Find matching buy based on FIFO/LIFO
                        BuyPoolItem buyItem = "LIFO".equalsIgnoreCase(method) ? buyPool.getLast() : buyPool.getFirst();

                        BigDecimal matchedAmount = remainingSellAmount.min(buyItem.remainingAmount);

                        // Convert prices and fees to PLN at historic rate (using exchangeRateService)
                        BigDecimal buyPricePln = exchangeRateService.convert(buyItem.price, buyItem.currency, "PLN");
                        BigDecimal sellPricePln = exchangeRateService.convert(sellPrice, sellCurrency, "PLN");
                        
                        BigDecimal buyUnitFeePln = exchangeRateService.convert(buyItem.unitFee, buyItem.currency, "PLN");
                        BigDecimal sellUnitFeePln = exchangeRateService.convert(unitSellFee, sellCurrency, "PLN");

                        BigDecimal matchedBuyCostPln = matchedAmount.multiply(buyPricePln);
                        BigDecimal matchedSellRevenuePln = matchedAmount.multiply(sellPricePln);
                        
                        BigDecimal matchedBuyFeePln = matchedAmount.multiply(buyUnitFeePln);
                        BigDecimal matchedSellFeePln = matchedAmount.multiply(sellUnitFeePln);
                        BigDecimal totalMatchedFeePln = matchedBuyFeePln.add(matchedSellFeePln);

                        // In Poland: przychód is just revenue, and koszt is buy cost + fees
                        BigDecimal costOfAcquisitionPln = matchedBuyCostPln.add(totalMatchedFeePln);
                        BigDecimal profitPln = matchedSellRevenuePln.subtract(costOfAcquisitionPln);

                        // Only add to report if the sell occurred in the requested year
                        if (trade.date.getYear() == year) {
                            allRows.add(new TaxRow(
                                    symbol,
                                    assetType,
                                    buyItem.date,
                                    trade.date,
                                    matchedAmount.setScale(6, RoundingMode.HALF_UP),
                                    buyPricePln.setScale(2, RoundingMode.HALF_UP),
                                    sellPricePln.setScale(2, RoundingMode.HALF_UP),
                                    matchedSellRevenuePln.setScale(2, RoundingMode.HALF_UP),
                                    costOfAcquisitionPln.setScale(2, RoundingMode.HALF_UP),
                                    totalMatchedFeePln.setScale(2, RoundingMode.HALF_UP),
                                    profitPln.setScale(2, RoundingMode.HALF_UP)
                            ));
                        }

                        // Update remaining amounts
                        buyItem.remainingAmount = buyItem.remainingAmount.subtract(matchedAmount);
                        remainingSellAmount = remainingSellAmount.subtract(matchedAmount);

                        if (buyItem.remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
                            if ("LIFO".equalsIgnoreCase(method)) {
                                buyPool.removeLast();
                            } else {
                                buyPool.removeFirst();
                            }
                        }
                    }
                }
            }
        }

        // Calculate totals
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalFees = BigDecimal.ZERO;
        BigDecimal netProfit = BigDecimal.ZERO;

        for (TaxRow row : allRows) {
            totalRevenue = totalRevenue.add(row.getRevenue());
            totalCost = totalCost.add(row.getCost());
            totalFees = totalFees.add(row.getFee());
            netProfit = netProfit.add(row.getProfit());
        }

        // Polish tax rate is 19%
        BigDecimal estimatedTax = BigDecimal.ZERO;
        if (netProfit.compareTo(BigDecimal.ZERO) > 0) {
            estimatedTax = netProfit.multiply(BigDecimal.valueOf(0.19)).setScale(2, RoundingMode.HALF_UP);
        }

        return new TaxReportDto(
                totalRevenue.setScale(2, RoundingMode.HALF_UP),
                totalCost.setScale(2, RoundingMode.HALF_UP),
                totalFees.setScale(2, RoundingMode.HALF_UP),
                netProfit.setScale(2, RoundingMode.HALF_UP),
                estimatedTax,
                allRows
        );
    }

    @Override
    public byte[] exportReportToCsv(TaxReportDto report) {
        StringBuilder csv = new StringBuilder();
        csv.append("Symbol,Typ aktywa,Data zakupu,Data sprzedazy,Ilosc,Cena zakupu,Cena sprzedazy,Przychod,Koszt zakupu (z prowizjami),Prowizje,Zysk/Strata\n");
        for (TaxRow r : report.getRows()) {
            csv.append(String.format(Locale.US, "%s,%s,%s,%s,%.6f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f\n",
                    r.getSymbol(),
                    r.getAssetType(),
                    r.getBuyDate().toString(),
                    r.getSellDate().toString(),
                    r.getAmount().doubleValue(),
                    r.getBuyPrice().doubleValue(),
                    r.getSellPrice().doubleValue(),
                    r.getRevenue().doubleValue(),
                    r.getCost().doubleValue(),
                    r.getFee().doubleValue(),
                    r.getProfit().doubleValue()
            ));
        }
        return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
}
