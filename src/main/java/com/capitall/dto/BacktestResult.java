package com.capitall.dto;

import java.math.BigDecimal;
import java.util.List;

public class BacktestResult {

    // ── Inner: single OHLCV bar (used internally + exposed for chart) ──────
    public static class OHLCVBar {
        public final String date;
        public final double open;
        public final double high;
        public final double low;
        public final double close;
        public final long   volume;

        public OHLCVBar(String date, double open, double high, double low, double close, long volume) {
            this.date   = date;
            this.open   = open;
            this.high   = high;
            this.low    = low;
            this.close  = close;
            this.volume = volume;
        }
    }

    // ── Inner: one simulated trade ─────────────────────────────────────────
    public static class BacktestTrade {
        public final String date;
        public final String side;   // "BUY" or "SELL"
        public final double price;
        public final double shares;
        public final double value;
        public final double pnl;    // realized P&L on SELL (0 for BUY)

        public BacktestTrade(String date, String side, double price, double shares, double value, double pnl) {
            this.date   = date;
            this.side   = side;
            this.price  = price;
            this.shares = shares;
            this.value  = value;
            this.pnl    = pnl;
        }
    }

    // ── Inner: single point on equity curve ───────────────────────────────
    public static class EquityPoint {
        public final String date;
        public final double equity;
        public final double price;

        public EquityPoint(String date, double equity, double price) {
            this.date   = date;
            this.equity = equity;
            this.price  = price;
        }
    }

    // ── Result fields ──────────────────────────────────────────────────────
    private final String symbol;
    private final String strategy;
    private final String period;

    private final double initialCapital;
    private final double finalEquity;
    private final double totalReturnPct;
    private final double maxDrawdownPct;
    private final double sharpeRatio;
    private final double winRate;
    private final int    totalTrades;
    private final int    winningTrades;

    private final List<BacktestTrade> trades;
    private final List<EquityPoint>   equityCurve;
    private final List<OHLCVBar>      ohlcv;

    // ── Constructor ────────────────────────────────────────────────────────
    public BacktestResult(String symbol, String strategy, String period,
                          double initialCapital, double finalEquity,
                          double totalReturnPct, double maxDrawdownPct,
                          double sharpeRatio, double winRate,
                          int totalTrades, int winningTrades,
                          List<BacktestTrade> trades,
                          List<EquityPoint> equityCurve,
                          List<OHLCVBar> ohlcv) {
        this.symbol         = symbol;
        this.strategy       = strategy;
        this.period         = period;
        this.initialCapital = initialCapital;
        this.finalEquity    = finalEquity;
        this.totalReturnPct = totalReturnPct;
        this.maxDrawdownPct = maxDrawdownPct;
        this.sharpeRatio    = sharpeRatio;
        this.winRate        = winRate;
        this.totalTrades    = totalTrades;
        this.winningTrades  = winningTrades;
        this.trades         = trades;
        this.equityCurve    = equityCurve;
        this.ohlcv          = ohlcv;
    }

    // ── Getters ────────────────────────────────────────────────────────────
    public String getSymbol()         { return symbol; }
    public String getStrategy()       { return strategy; }
    public String getPeriod()         { return period; }
    public double getInitialCapital() { return initialCapital; }
    public double getFinalEquity()    { return finalEquity; }
    public double getTotalReturnPct() { return totalReturnPct; }
    public double getMaxDrawdownPct() { return maxDrawdownPct; }
    public double getSharpeRatio()    { return sharpeRatio; }
    public double getWinRate()        { return winRate; }
    public int    getTotalTrades()    { return totalTrades; }
    public int    getWinningTrades()  { return winningTrades; }
    public List<BacktestTrade> getTrades()      { return trades; }
    public List<EquityPoint>   getEquityCurve() { return equityCurve; }
    public List<OHLCVBar>      getOhlcv()       { return ohlcv; }
}
