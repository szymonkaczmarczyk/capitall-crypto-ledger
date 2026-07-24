package com.capitall.service;

import com.capitall.dto.BacktestRequest;
import com.capitall.dto.BacktestResult;
import com.capitall.dto.BacktestResult.BacktestTrade;
import com.capitall.dto.BacktestResult.EquityPoint;
import com.capitall.dto.BacktestResult.OHLCVBar;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class BacktestService {

    private final SecuritiesPriceService priceService;

    public BacktestService(SecuritiesPriceService priceService) {
        this.priceService = priceService;
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Public entry point
    // ─────────────────────────────────────────────────────────────────────

    public BacktestResult run(BacktestRequest req) {
        List<OHLCVBar> bars = priceService.fetchOHLCV(
                req.getSymbol(),
                req.getPeriod().yahooRange,
                req.getPeriod().yahooInterval
        );

        if (bars.size() < 5) {
            throw new IllegalArgumentException(
                    "Nie udało się pobrać wystarczającej liczby danych historycznych dla symbolu: "
                    + req.getSymbol());
        }

        return switch (req.getStrategy()) {
            case SMA_CROSSOVER  -> runSma(req, bars);
            case RSI            -> runRsi(req, bars);
            case BOLLINGER_BANDS -> runBollinger(req, bars);
        };
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Strategy: SMA Crossover
    // ─────────────────────────────────────────────────────────────────────

    private BacktestResult runSma(BacktestRequest req, List<OHLCVBar> bars) {
        int shortP = req.getSmaShort();
        int longP  = req.getSmaLong();

        double[] closes = closePrices(bars);
        double[] smaS   = sma(closes, shortP);
        double[] smaL   = sma(closes, longP);

        SimState sim = new SimState(req.getInitialCapital().doubleValue(), bars);
        int start = Math.max(shortP, longP);

        for (int i = start; i < bars.size(); i++) {
            boolean prevBull = smaS[i - 1] > smaL[i - 1];
            boolean curBull  = smaS[i]     > smaL[i];

            if (!prevBull && curBull) {
                sim.buy(i);
            } else if (prevBull && !curBull) {
                sim.sell(i);
            }
        }

        sim.closePosition(bars.size() - 1);
        return buildResult(req, bars, sim,
                "SMA Crossover (" + shortP + "/" + longP + ")");
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Strategy: RSI
    // ─────────────────────────────────────────────────────────────────────

    private BacktestResult runRsi(BacktestRequest req, List<OHLCVBar> bars) {
        int    period     = req.getRsiPeriod();
        double oversold   = req.getRsiOversold();
        double overbought = req.getRsiOverbought();

        double[] closes = closePrices(bars);
        double[] rsi    = rsi(closes, period);

        SimState sim = new SimState(req.getInitialCapital().doubleValue(), bars);

        for (int i = period + 1; i < bars.size(); i++) {
            if (rsi[i - 1] <= oversold && sim.shares == 0) {
                sim.buy(i);
            } else if (rsi[i - 1] >= overbought && sim.shares > 0) {
                sim.sell(i);
            }
        }

        sim.closePosition(bars.size() - 1);
        return buildResult(req, bars, sim,
                "RSI (" + period + ", " + (int)oversold + "/" + (int)overbought + ")");
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Strategy: Bollinger Bands
    // ─────────────────────────────────────────────────────────────────────

    private BacktestResult runBollinger(BacktestRequest req, List<OHLCVBar> bars) {
        int    period = req.getBbPeriod();
        double stdDev = req.getBbStdDev();

        double[] closes = closePrices(bars);
        double[] mid    = sma(closes, period);
        double[] upper  = new double[closes.length];
        double[] lower  = new double[closes.length];

        for (int i = period - 1; i < closes.length; i++) {
            double mean = mid[i];
            double variance = 0;
            for (int j = i - period + 1; j <= i; j++) {
                variance += Math.pow(closes[j] - mean, 2);
            }
            double sd = Math.sqrt(variance / period);
            upper[i] = mean + stdDev * sd;
            lower[i] = mean - stdDev * sd;
        }

        SimState sim = new SimState(req.getInitialCapital().doubleValue(), bars);

        for (int i = period; i < bars.size(); i++) {
            double price = bars.get(i).close;
            if (price < lower[i] && sim.shares == 0) {
                sim.buy(i);
            } else if (price > upper[i] && sim.shares > 0) {
                sim.sell(i);
            }
        }

        sim.closePosition(bars.size() - 1);
        return buildResult(req, bars, sim,
                "Bollinger Bands (" + period + ", " + stdDev + "σ)");
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Result builder
    // ─────────────────────────────────────────────────────────────────────

    private BacktestResult buildResult(BacktestRequest req, List<OHLCVBar> bars,
                                       SimState sim, String strategyLabel) {
        double initial     = req.getInitialCapital().doubleValue();
        double finalEquity = sim.currentEquity(bars.size() - 1);
        double totalReturn = (finalEquity - initial) / initial * 100.0;
        double maxDrawdown = computeMaxDrawdown(sim.equityCurve);
        double sharpe      = computeSharpe(sim.equityCurve);
        int    wins        = (int) sim.trades.stream().filter(t -> t.pnl > 0).count();
        int    sells       = (int) sim.trades.stream().filter(t -> t.side.equals("SELL")).count();
        double winRate     = sells > 0 ? (double) wins / sells * 100.0 : 0;

        return new BacktestResult(
                req.getSymbol().toUpperCase(),
                strategyLabel,
                req.getPeriod().yahooRange,
                initial,
                finalEquity,
                totalReturn,
                maxDrawdown,
                sharpe,
                winRate,
                sim.trades.size(),
                wins,
                sim.trades,
                sim.equityCurve,
                bars
        );
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Simulation state
    // ─────────────────────────────────────────────────────────────────────

    private static class SimState {
        double cash;
        double shares = 0;
        double entryPrice = 0;
        final List<OHLCVBar>    bars;
        final List<BacktestTrade> trades     = new ArrayList<>();
        final List<EquityPoint>   equityCurve = new ArrayList<>();

        SimState(double initialCash, List<OHLCVBar> bars) {
            this.cash = initialCash;
            this.bars = bars;
        }

        void buy(int idx) {
            if (shares > 0) return; // already in
            double price = bars.get(idx).close;
            shares = cash / price;
            cash = 0;
            entryPrice = price;
            trades.add(new BacktestTrade(bars.get(idx).date, "BUY", price, shares, shares * price, 0));
            recordEquity(idx);
        }

        void sell(int idx) {
            if (shares == 0) return;
            double price = bars.get(idx).close;
            double value = shares * price;
            double pnl   = value - (shares * entryPrice);
            cash = value;
            trades.add(new BacktestTrade(bars.get(idx).date, "SELL", price, shares, value, pnl));
            shares = 0;
            entryPrice = 0;
            recordEquity(idx);
        }

        void closePosition(int lastIdx) {
            if (shares > 0) sell(lastIdx);
            // build full equity curve if not yet complete
            if (equityCurve.size() < bars.size()) {
                for (int i = 0; i < bars.size(); i++) {
                    final String date = bars.get(i).date;
                    final int    idx  = i;
                    if (equityCurve.stream().noneMatch(e -> e.date.equals(date))) {
                        equityCurve.add(new EquityPoint(date,
                                currentEquity(idx), bars.get(idx).close));
                    }
                }
                equityCurve.sort((a, b) -> a.date.compareTo(b.date));
            }
        }


        double currentEquity(int idx) {
            return cash + shares * bars.get(idx).close;
        }

        private void recordEquity(int idx) {
            equityCurve.add(new EquityPoint(bars.get(idx).date, currentEquity(idx), bars.get(idx).close));
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Technical indicators
    // ─────────────────────────────────────────────────────────────────────

    /** Simple Moving Average — result[i] is the SMA of period 'p' ending at index i.
     *  Values before the first full window are set to 0. */
    private static double[] sma(double[] data, int p) {
        double[] result = new double[data.length];
        double   sum    = 0;
        for (int i = 0; i < data.length; i++) {
            sum += data[i];
            if (i >= p) sum -= data[i - p];
            result[i] = (i >= p - 1) ? sum / p : 0;
        }
        return result;
    }

    /** Wilder RSI */
    private static double[] rsi(double[] data, int period) {
        double[] rsi    = new double[data.length];
        double   avgGain = 0, avgLoss = 0;

        // Initial average gain/loss over first 'period' changes
        for (int i = 1; i <= period && i < data.length; i++) {
            double delta = data[i] - data[i - 1];
            if (delta > 0) avgGain += delta;
            else           avgLoss -= delta;
        }
        avgGain /= period;
        avgLoss /= period;

        if (period < data.length) {
            rsi[period] = (avgLoss == 0) ? 100 : 100 - 100.0 / (1 + avgGain / avgLoss);
        }

        for (int i = period + 1; i < data.length; i++) {
            double delta = data[i] - data[i - 1];
            double gain  = Math.max(delta, 0);
            double loss  = Math.max(-delta, 0);
            avgGain = (avgGain * (period - 1) + gain) / period;
            avgLoss = (avgLoss * (period - 1) + loss) / period;
            rsi[i] = (avgLoss == 0) ? 100 : 100 - 100.0 / (1 + avgGain / avgLoss);
        }
        return rsi;
    }

    private static double[] closePrices(List<OHLCVBar> bars) {
        double[] arr = new double[bars.size()];
        for (int i = 0; i < bars.size(); i++) arr[i] = bars.get(i).close;
        return arr;
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Metrics
    // ─────────────────────────────────────────────────────────────────────

    private static double computeMaxDrawdown(List<EquityPoint> curve) {
        double peak = Double.NEGATIVE_INFINITY;
        double maxDD = 0;
        for (EquityPoint ep : curve) {
            if (ep.equity > peak) peak = ep.equity;
            double dd = (peak - ep.equity) / peak * 100.0;
            if (dd > maxDD) maxDD = dd;
        }
        return maxDD;
    }

    /** Annualised Sharpe ratio (risk-free rate assumed 0, daily returns) */
    private static double computeSharpe(List<EquityPoint> curve) {
        if (curve.size() < 2) return 0;
        List<Double> returns = new ArrayList<>();
        for (int i = 1; i < curve.size(); i++) {
            double prev = curve.get(i - 1).equity;
            double curr = curve.get(i).equity;
            if (prev > 0) returns.add((curr - prev) / prev);
        }
        if (returns.isEmpty()) return 0;
        double mean = returns.stream().mapToDouble(d -> d).average().orElse(0);
        double variance = returns.stream().mapToDouble(r -> Math.pow(r - mean, 2))
                .average().orElse(0);
        double std = Math.sqrt(variance);
        if (std == 0) return 0;
        return (mean / std) * Math.sqrt(252); // annualise
    }
}
