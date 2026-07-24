package com.capitall.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class TaxReportDto {
    private BigDecimal totalRevenue;
    private BigDecimal totalCost;
    private BigDecimal totalFees;
    private BigDecimal netProfit;
    private BigDecimal estimatedTax;
    private List<TaxRow> rows;

    public TaxReportDto() {}

    public TaxReportDto(BigDecimal totalRevenue, BigDecimal totalCost, BigDecimal totalFees, BigDecimal netProfit, BigDecimal estimatedTax, List<TaxRow> rows) {
        this.totalRevenue = totalRevenue;
        this.totalCost = totalCost;
        this.totalFees = totalFees;
        this.netProfit = netProfit;
        this.estimatedTax = estimatedTax;
        this.rows = rows;
    }

    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }

    public BigDecimal getTotalCost() { return totalCost; }
    public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }

    public BigDecimal getTotalFees() { return totalFees; }
    public void setTotalFees(BigDecimal totalFees) { this.totalFees = totalFees; }

    public BigDecimal getNetProfit() { return netProfit; }
    public void setNetProfit(BigDecimal netProfit) { this.netProfit = netProfit; }

    public BigDecimal getEstimatedTax() { return estimatedTax; }
    public void setEstimatedTax(BigDecimal estimatedTax) { this.estimatedTax = estimatedTax; }

    public List<TaxRow> getRows() { return rows; }
    public void setRows(List<TaxRow> rows) { this.rows = rows; }

    public static class TaxRow {
        private String symbol;
        private String assetType;
        private LocalDateTime buyDate;
        private LocalDateTime sellDate;
        private BigDecimal amount;
        private BigDecimal buyPrice;
        private BigDecimal sellPrice;
        private BigDecimal revenue;
        private BigDecimal cost;
        private BigDecimal fee;
        private BigDecimal profit;

        public TaxRow() {}

        public TaxRow(String symbol, String assetType, LocalDateTime buyDate, LocalDateTime sellDate, BigDecimal amount, BigDecimal buyPrice, BigDecimal sellPrice, BigDecimal revenue, BigDecimal cost, BigDecimal fee, BigDecimal profit) {
            this.symbol = symbol;
            this.assetType = assetType;
            this.buyDate = buyDate;
            this.sellDate = sellDate;
            this.amount = amount;
            this.buyPrice = buyPrice;
            this.sellPrice = sellPrice;
            this.revenue = revenue;
            this.cost = cost;
            this.fee = fee;
            this.profit = profit;
        }

        public String getSymbol() { return symbol; }
        public void setSymbol(String symbol) { this.symbol = symbol; }

        public String getAssetType() { return assetType; }
        public void setAssetType(String assetType) { this.assetType = assetType; }

        public LocalDateTime getBuyDate() { return buyDate; }
        public void setBuyDate(LocalDateTime buyDate) { this.buyDate = buyDate; }

        public LocalDateTime getSellDate() { return sellDate; }
        public void setSellDate(LocalDateTime sellDate) { this.sellDate = sellDate; }

        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }

        public BigDecimal getBuyPrice() { return buyPrice; }
        public void setBuyPrice(BigDecimal buyPrice) { this.buyPrice = buyPrice; }

        public BigDecimal getSellPrice() { return sellPrice; }
        public void setSellPrice(BigDecimal sellPrice) { this.sellPrice = sellPrice; }

        public BigDecimal getRevenue() { return revenue; }
        public void setRevenue(BigDecimal revenue) { this.revenue = revenue; }

        public BigDecimal getCost() { return cost; }
        public void setCost(BigDecimal cost) { this.cost = cost; }

        public BigDecimal getFee() { return fee; }
        public void setFee(BigDecimal fee) { this.fee = fee; }

        public BigDecimal getProfit() { return profit; }
        public void setProfit(BigDecimal profit) { this.profit = profit; }
    }
}
