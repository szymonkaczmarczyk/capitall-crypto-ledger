package com.capitall.controller;

import com.capitall.service.MarketDataService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/market")
public class MarketApiController {

    private final MarketDataService marketDataService;

    public MarketApiController(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
    }

    @GetMapping("/overview")
    public Map<String, Object> getMarketOverview() {
        return marketDataService.getMarketOverview();
    }
}
