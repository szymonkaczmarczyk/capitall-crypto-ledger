package com.capitall.dto;

import java.util.List;
import java.util.Map;

public class PortfolioAnalyticsResponse {
    private double sharpeRatio;
    private double maxDrawdown;
    private double beta;
    private double valueAtRisk;
    
    private List<String> symbols;
    private double[][] correlationMatrix;
    private Map<String, Double> weights;

    public PortfolioAnalyticsResponse() {}

    public PortfolioAnalyticsResponse(double sharpeRatio, double maxDrawdown, double beta, double valueAtRisk,
                                      List<String> symbols, double[][] correlationMatrix, Map<String, Double> weights) {
        this.sharpeRatio = sharpeRatio;
        this.maxDrawdown = maxDrawdown;
        this.beta = beta;
        this.valueAtRisk = valueAtRisk;
        this.symbols = symbols;
        this.correlationMatrix = correlationMatrix;
        this.weights = weights;
    }

    public double getSharpeRatio() { return sharpeRatio; }
    public void setSharpeRatio(double sharpeRatio) { this.sharpeRatio = sharpeRatio; }

    public double getMaxDrawdown() { return maxDrawdown; }
    public void setMaxDrawdown(double maxDrawdown) { this.maxDrawdown = maxDrawdown; }

    public double getBeta() { return beta; }
    public void setBeta(double beta) { this.beta = beta; }

    public double getValueAtRisk() { return valueAtRisk; }
    public void setValueAtRisk(double valueAtRisk) { this.valueAtRisk = valueAtRisk; }

    public List<String> getSymbols() { return symbols; }
    public void setSymbols(List<String> symbols) { this.symbols = symbols; }

    public double[][] getCorrelationMatrix() { return correlationMatrix; }
    public void setCorrelationMatrix(double[][] correlationMatrix) { this.correlationMatrix = correlationMatrix; }

    public Map<String, Double> getWeights() { return weights; }
    public void setWeights(Map<String, Double> weights) { this.weights = weights; }
}
