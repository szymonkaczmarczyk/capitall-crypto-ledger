package com.capitall.dto;

import java.math.BigDecimal;

public class BacktestRequest {

    public enum Strategy {
        SMA_CROSSOVER, RSI, BOLLINGER_BANDS
    }

    public enum Period {
        ONE_MONTH("1mo", "1d"),
        THREE_MONTHS("3mo", "1d"),
        SIX_MONTHS("6mo", "1d"),
        ONE_YEAR("1y", "1d"),
        TWO_YEARS("2y", "1wk");

        public final String yahooRange;
        public final String yahooInterval;

        Period(String yahooRange, String yahooInterval) {
            this.yahooRange = yahooRange;
            this.yahooInterval = yahooInterval;
        }
    }

    private String symbol;
    private Strategy strategy;
    private Period period;
    private BigDecimal initialCapital;

    // SMA Crossover params
    private int smaShort = 10;
    private int smaLong  = 30;

    // RSI params
    private int rsiPeriod    = 14;
    private int rsiOversold  = 30;
    private int rsiOverbought = 70;

    // Bollinger Bands params
    private int    bbPeriod = 20;
    private double bbStdDev = 2.0;

    public BacktestRequest() {}

    // ── Getters & Setters ──────────────────────────────────────────────────

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public Strategy getStrategy() { return strategy; }
    public void setStrategy(Strategy strategy) { this.strategy = strategy; }

    public Period getPeriod() { return period; }
    public void setPeriod(Period period) { this.period = period; }

    public BigDecimal getInitialCapital() { return initialCapital; }
    public void setInitialCapital(BigDecimal initialCapital) { this.initialCapital = initialCapital; }

    public int getSmaShort() { return smaShort; }
    public void setSmaShort(int smaShort) { this.smaShort = smaShort; }

    public int getSmaLong() { return smaLong; }
    public void setSmaLong(int smaLong) { this.smaLong = smaLong; }

    public int getRsiPeriod() { return rsiPeriod; }
    public void setRsiPeriod(int rsiPeriod) { this.rsiPeriod = rsiPeriod; }

    public int getRsiOversold() { return rsiOversold; }
    public void setRsiOversold(int rsiOversold) { this.rsiOversold = rsiOversold; }

    public int getRsiOverbought() { return rsiOverbought; }
    public void setRsiOverbought(int rsiOverbought) { this.rsiOverbought = rsiOverbought; }

    public int getBbPeriod() { return bbPeriod; }
    public void setBbPeriod(int bbPeriod) { this.bbPeriod = bbPeriod; }

    public double getBbStdDev() { return bbStdDev; }
    public void setBbStdDev(double bbStdDev) { this.bbStdDev = bbStdDev; }
}
