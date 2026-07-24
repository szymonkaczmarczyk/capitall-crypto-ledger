package com.capitall.dto;

import java.math.BigDecimal;
import java.util.List;

public class RebalanceResultDto {
    private BigDecimal totalPortfolioValue;
    private List<AllocationDetail> currentAllocations;
    private List<TargetDetail> targetAllocations;
    private List<Recommendation> recommendations;

    public RebalanceResultDto() {}

    public RebalanceResultDto(BigDecimal totalPortfolioValue, List<AllocationDetail> currentAllocations, List<TargetDetail> targetAllocations, List<Recommendation> recommendations) {
        this.totalPortfolioValue = totalPortfolioValue;
        this.currentAllocations = currentAllocations;
        this.targetAllocations = targetAllocations;
        this.recommendations = recommendations;
    }

    public BigDecimal getTotalPortfolioValue() { return totalPortfolioValue; }
    public void setTotalPortfolioValue(BigDecimal totalPortfolioValue) { this.totalPortfolioValue = totalPortfolioValue; }

    public List<AllocationDetail> getCurrentAllocations() { return currentAllocations; }
    public void setCurrentAllocations(List<AllocationDetail> currentAllocations) { this.currentAllocations = currentAllocations; }

    public List<TargetDetail> getTargetAllocations() { return targetAllocations; }
    public void setTargetAllocations(List<TargetDetail> targetAllocations) { this.targetAllocations = targetAllocations; }

    public List<Recommendation> getRecommendations() { return recommendations; }
    public void setRecommendations(List<Recommendation> recommendations) { this.recommendations = recommendations; }

    public static class AllocationDetail {
        private String symbol;
        private BigDecimal amount;
        private BigDecimal price;
        private BigDecimal value;
        private BigDecimal percentage; // e.g. 45.50 (%)

        public AllocationDetail() {}

        public AllocationDetail(String symbol, BigDecimal amount, BigDecimal price, BigDecimal value, BigDecimal percentage) {
            this.symbol = symbol;
            this.amount = amount;
            this.price = price;
            this.value = value;
            this.percentage = percentage;
        }

        public String getSymbol() { return symbol; }
        public void setSymbol(String symbol) { this.symbol = symbol; }

        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }

        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }

        public BigDecimal getValue() { return value; }
        public void setValue(BigDecimal value) { this.value = value; }

        public BigDecimal getPercentage() { return percentage; }
        public void setPercentage(BigDecimal percentage) { this.percentage = percentage; }
    }

    public static class TargetDetail {
        private String symbol;
        private BigDecimal targetPercentage; // e.g. 50.00 (%)
        private BigDecimal targetValue;
        private BigDecimal diffValue; // targetValue - currentValue

        public TargetDetail() {}

        public TargetDetail(String symbol, BigDecimal targetPercentage, BigDecimal targetValue, BigDecimal diffValue) {
            this.symbol = symbol;
            this.targetPercentage = targetPercentage;
            this.targetValue = targetValue;
            this.diffValue = diffValue;
        }

        public String getSymbol() { return symbol; }
        public void setSymbol(String symbol) { this.symbol = symbol; }

        public BigDecimal getTargetPercentage() { return targetPercentage; }
        public void setTargetPercentage(BigDecimal targetPercentage) { this.targetPercentage = targetPercentage; }

        public BigDecimal getTargetValue() { return targetValue; }
        public void setTargetValue(BigDecimal targetValue) { this.targetValue = targetValue; }

        public BigDecimal getDiffValue() { return diffValue; }
        public void setDiffValue(BigDecimal diffValue) { this.diffValue = diffValue; }
    }

    public static class Recommendation {
        private String symbol;
        private String action; // BUY, SELL, HOLD
        private BigDecimal quantity;
        private BigDecimal valueInUsd;

        public Recommendation() {}

        public Recommendation(String symbol, String action, BigDecimal quantity, BigDecimal valueInUsd) {
            this.symbol = symbol;
            this.action = action;
            this.quantity = quantity;
            this.valueInUsd = valueInUsd;
        }

        public String getSymbol() { return symbol; }
        public void setSymbol(String symbol) { this.symbol = symbol; }

        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }

        public BigDecimal getQuantity() { return quantity; }
        public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

        public BigDecimal getValueInUsd() { return valueInUsd; }
        public void setValueInUsd(BigDecimal valueInUsd) { this.valueInUsd = valueInUsd; }
    }
}
