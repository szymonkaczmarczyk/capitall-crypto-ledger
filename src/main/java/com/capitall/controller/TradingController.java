package com.capitall.controller;

import com.capitall.dto.BacktestResult;
import com.capitall.model.SimulatedTrade;
import com.capitall.model.SimulatedWallet;
import com.capitall.model.User;
import com.capitall.repository.SimulatedHoldingRepository;
import com.capitall.service.SecuritiesPriceService;
import com.capitall.service.SimulatorService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/trading")
public class TradingController {

    private final SimulatorService simulatorService;
    private final SecuritiesPriceService priceService;
    private final SimulatedHoldingRepository holdingRepository;

    private static final List<Map<String, String>> POPULAR_STOCKS = List.of(
        Map.of("symbol", "AAPL", "name", "Apple Inc.", "currency", "USD", "category", "US Tech", "sector", "Technologia / Elektronika", "desc", "Projektant iPhone, Mac oraz dostawca zaawansowanego oprogramowania i usług cyfrowych."),
        Map.of("symbol", "TSLA", "name", "Tesla Inc.", "currency", "USD", "category", "US Tech", "sector", "Motoryzacja / Czysta Energia", "desc", "Lider rynku pojazdów elektrycznych i innowacyjnych systemów przechowywania energii fotowoltaicznej."),
        Map.of("symbol", "NVDA", "name", "NVIDIA Corp.", "currency", "USD", "category", "US Tech", "sector", "Półprzewodniki / AI", "desc", "Światowy gigant w produkcji układów scalonych i procesorów graficznych (GPU) napędzających sztuczną inteligencję."),
        Map.of("symbol", "MSFT", "name", "Microsoft Corp.", "currency", "USD", "category", "US Tech", "sector", "Oprogramowanie / Chmura", "desc", "Twórca systemu Windows, pakietu Office oraz dostawca usług chmurowych Azure i partner OpenAI."),
        Map.of("symbol", "AMZN", "name", "Amazon.com Inc.", "currency", "USD", "category", "US Tech", "sector", "E-Commerce / Chmura AWS", "desc", "Globalny lider handlu internetowego oraz największy na świecie dostawca infrastruktury chmurowej AWS."),
        Map.of("symbol", "GOOGL", "name", "Alphabet Inc.", "currency", "USD", "category", "US Tech", "sector", "Usługi Internetowe / AI", "desc", "Właściciel wyszukiwarki Google, platformy YouTube, systemu Android i innowacji Gemini AI."),
        Map.of("symbol", "META", "name", "Meta Platforms", "currency", "USD", "category", "US Tech", "sector", "Media Społecznościowe", "desc", "Właściciel serwisów Facebook, Instagram, WhatsApp oraz pionier VR/AR."),
        Map.of("symbol", "NFLX", "name", "Netflix Inc.", "currency", "USD", "category", "US Tech", "sector", "Rozrywka / Streaming", "desc", "Największy na świecie serwis wideo na żądanie z produkcjami filmowymi i serialowymi."),
        Map.of("symbol", "AMD", "name", "Advanced Micro Devices", "currency", "USD", "category", "US Tech", "sector", "Półprzewodniki", "desc", "Producent wydajnych procesorów Ryzen i EPYC oraz układów graficznych Radeon."),
        Map.of("symbol", "INTC", "name", "Intel Corp.", "currency", "USD", "category", "US Tech", "sector", "Półprzewodniki", "desc", "Czołowy producent procesorów komputerowych x86 oraz inwestor w fabryki mikroprocesorów."),
        Map.of("symbol", "PLTR", "name", "Palantir Tech", "currency", "USD", "category", "US Tech", "sector", "Analiza Danych / AI", "desc", "Amerykańskie przedsiębiorstwo analityczne dostarczające platformy AI dla wywiadu, wojska i przedsiębiorstw."),
        Map.of("symbol", "DIS", "name", "Walt Disney Co.", "currency", "USD", "category", "US Tech", "sector", "Rozrywka / Media", "desc", "Światowy koncern mediowy, właściciel parków rozrywki, studium filmowego Pixar, Marvel oraz Disney+."),
        Map.of("symbol", "V", "name", "Visa Inc.", "currency", "USD", "category", "US Tech", "sector", "Finanse / Płatności", "desc", "Międzynarodowy lider cyfrowych płatności bezgotówkowych i infrastruktury kart płatniczych."),
        Map.of("symbol", "JPM", "name", "JPMorgan Chase", "currency", "USD", "category", "US Tech", "sector", "Bankowość Inwestycyjna", "desc", "Największy amerykański bank i instytucja finansowa świadcząca usługi dla klientów globalnych."),
        Map.of("symbol", "CDR", "name", "CD Projekt SA", "currency", "PLN", "category", "GPW Warszawa", "sector", "Gry Komputerowe", "desc", "Czołowy polski producent gier wideo, twórca światowych hitów Wiedźmin 3 oraz Cyberpunk 2077."),
        Map.of("symbol", "PKO", "name", "PKO Bank Polski", "currency", "PLN", "category", "GPW Warszawa", "sector", "Bankowość / Finanse", "desc", "Największy bank uniwersalny w Polsce i Europie Środkowo-Wschodniej ze stuletnią tradycją."),
        Map.of("symbol", "PKN", "name", "Orlen SA", "currency", "PLN", "category", "GPW Warszawa", "sector", "Energetyka / Paliwa", "desc", "Multienergetyczny koncern działający na rynku paliw, gazu, energii elektrycznej i OZE."),
        Map.of("symbol", "KGH", "name", "KGHM Polska Miedź", "currency", "PLN", "category", "GPW Warszawa", "sector", "Górnictwo / Metale", "desc", "Jeden z czołowych światowych producentów miedzi rafinowanej i srebra z kopalniami na świecie."),
        Map.of("symbol", "LPP", "name", "LPP SA", "currency", "PLN", "category", "GPW Warszawa", "sector", "Odzież / Moda", "desc", "Polskie przedsiębiorstwo odzieżowe, właściciel marek Reserved, Cropp, House, Mohito i Sinsay."),
        Map.of("symbol", "DNP", "name", "Dino Polska SA", "currency", "PLN", "category", "GPW Warszawa", "sector", "Handel Detaliczny", "desc", "Polska sieć supermarketów i jeden z najszybciej rozwijających się detalistów spożywczych w kraju."),
        Map.of("symbol", "ALE", "name", "Allegro.eu SA", "currency", "PLN", "category", "GPW Warszawa", "sector", "E-Commerce", "desc", "Największa polska platforma handlowa e-commerce z milionami aktywnego kupujących."),
        Map.of("symbol", "SPY", "name", "SPDR S&P 500 ETF", "currency", "USD", "category", "ETF-y", "sector", "Indeks S&P 500", "desc", "Najstarszy i największy fundusz ETF na świecie śledzący indeks 500 największych amerykańskich spółek."),
        Map.of("symbol", "QQQ", "name", "Invesco QQQ ETF", "currency", "USD", "category", "ETF-y", "sector", "Indeks NASDAQ-100", "desc", "Fundusz ETF odzwierciedlający wyniki 100 największych spółek technologicznych notowanych na NASDAQ.")
    );

    public TradingController(SimulatorService simulatorService,
                             SecuritiesPriceService priceService,
                             SimulatedHoldingRepository holdingRepository) {
        this.simulatorService = simulatorService;
        this.priceService = priceService;
        this.holdingRepository = holdingRepository;
    }

    @GetMapping
    public String showTradingPage(Model model, @AuthenticationPrincipal User user) {
        if (user == null) {
            return "redirect:/login";
        }

        SimulatedWallet wallet = simulatorService.getOrCreateWallet(user.getId());
        List<SimulatorService.SimulatedHoldingSummary> allHoldings = simulatorService.getHoldingsSummary(user.getId());

        // FILTER: Only show stock holdings (exclude crypto holdings from simulator)
        Set<String> stockSymbols = POPULAR_STOCKS.stream()
                .map(s -> s.get("symbol"))
                .collect(Collectors.toSet());

        List<SimulatorService.SimulatedHoldingSummary> stockHoldings = allHoldings.stream()
                .filter(h -> stockSymbols.contains(h.getHolding().getSymbol()))
                .collect(Collectors.toList());

        model.addAttribute("wallet", wallet);
        model.addAttribute("holdings", stockHoldings);
        model.addAttribute("stocks", POPULAR_STOCKS);
        model.addAttribute("traderName", user.getUsername());

        return "trading";
    }

    @GetMapping("/api/ohlcv")
    @ResponseBody
    public Map<String, Object> getOHLCVData(@RequestParam(defaultValue = "AAPL") String symbol,
                                            @RequestParam(defaultValue = "1mo") String range,
                                            @RequestParam(defaultValue = "1d") String interval) {
        try {
            List<BacktestResult.OHLCVBar> bars = priceService.fetchOHLCV(symbol, range, interval);
            if (bars == null || bars.isEmpty()) {
                return Map.of("success", false, "error", "Brak danych świecowych dla symbolu: " + symbol);
            }

            List<Map<String, Object>> formattedBars = new ArrayList<>();
            for (BacktestResult.OHLCVBar b : bars) {
                Map<String, Object> bar = new HashMap<>();
                bar.put("time", b.date.toString());
                bar.put("open", b.open);
                bar.put("high", b.high);
                bar.put("low", b.low);
                bar.put("close", b.close);
                bar.put("volume", b.volume);
                formattedBars.add(bar);
            }

            return Map.of(
                "success", true,
                "symbol", symbol.toUpperCase(),
                "bars", formattedBars
            );
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage() != null ? e.getMessage() : "Błąd pobierania danych świecowych");
        }
    }

    @GetMapping("/api/ticker")
    @ResponseBody
    public Map<String, Object> getTickerData(@RequestParam(defaultValue = "AAPL") String symbol) {
        try {
            String cleanSym = symbol.toUpperCase().trim();
            BigDecimal price = priceService.getCurrentPrice(cleanSym);
            if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
                return Map.of("success", false, "error", "Nie udało się pobrać aktualnego kursu.");
            }

            List<BacktestResult.OHLCVBar> recentBars = priceService.fetchOHLCV(cleanSym, "5d", "1d");
            BigDecimal changePercent = BigDecimal.ZERO;
            BigDecimal high24h = price;
            BigDecimal low24h = price;

            if (recentBars != null && recentBars.size() >= 2) {
                BacktestResult.OHLCVBar prev = recentBars.get(recentBars.size() - 2);
                BacktestResult.OHLCVBar curr = recentBars.get(recentBars.size() - 1);
                if (prev.close > 0) {
                    double pct = ((curr.close - prev.close) / prev.close) * 100.0;
                    changePercent = BigDecimal.valueOf(pct).setScale(2, RoundingMode.HALF_UP);
                }
                high24h = BigDecimal.valueOf(curr.high).setScale(2, RoundingMode.HALF_UP);
                low24h = BigDecimal.valueOf(curr.low).setScale(2, RoundingMode.HALF_UP);
            }

            return Map.of(
                "success", true,
                "symbol", cleanSym,
                "price", price,
                "changePercent", changePercent,
                "high24h", high24h,
                "low24h", low24h
            );
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    @GetMapping("/api/details")
    @ResponseBody
    public Map<String, Object> getCompanyDetails(@RequestParam(defaultValue = "AAPL") String symbol) {
        try {
            String cleanSym = symbol.toUpperCase().trim();
            Map<String, String> meta = POPULAR_STOCKS.stream()
                    .filter(s -> s.get("symbol").equalsIgnoreCase(cleanSym))
                    .findFirst()
                    .orElse(Map.of("symbol", cleanSym, "name", cleanSym, "currency", "USD", "category", "Giełda", "sector", "Ogólny", "desc", "Spółka akcyjna notowana na giełdzie papierów wartościowych."));

            BigDecimal price = priceService.getCurrentPrice(cleanSym);
            if (price == null) price = new BigDecimal("150.00");

            List<BacktestResult.OHLCVBar> bars = priceService.fetchOHLCV(cleanSym, "1y", "1wk");
            double low52 = price.doubleValue() * 0.82;
            double high52 = price.doubleValue() * 1.28;
            long avgVolume = 12500000L;

            if (bars != null && !bars.isEmpty()) {
                low52 = bars.stream().mapToDouble(b -> b.low).min().orElse(low52);
                high52 = bars.stream().mapToDouble(b -> b.high).max().orElse(high52);
                avgVolume = (long) bars.stream().mapToLong(b -> b.volume).average().orElse(avgVolume);
            }

            double peRatio = 21.5 + (Math.abs(cleanSym.hashCode()) % 15);
            String marketCap = String.format("%.2f mld %s", (price.doubleValue() * 12.8 / 10), meta.get("currency"));
            double divYield = 1.1 + (Math.abs(cleanSym.hashCode()) % 20) / 10.0;

            Map<String, Object> details = new HashMap<>();
            details.put("success", true);
            details.put("symbol", cleanSym);
            details.put("name", meta.get("name"));
            details.put("currency", meta.get("currency"));
            details.put("category", meta.get("category"));
            details.put("sector", meta.get("sector"));
            details.put("desc", meta.get("desc"));
            details.put("price", price.doubleValue());
            details.put("peRatio", String.format("%.1f", peRatio));
            details.put("marketCap", marketCap);
            details.put("divYield", String.format("%.2f%%", divYield));
            details.put("low52", Math.round(low52 * 100.0) / 100.0);
            details.put("high52", Math.round(high52 * 100.0) / 100.0);
            details.put("avgVolume", String.format("%,d", avgVolume));

            return details;
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage() != null ? e.getMessage() : "Błąd pobierania danych spółki");
        }
    }

    @GetMapping("/api/marquee")
    @ResponseBody
    public Map<String, Object> getMarqueeItems() {
        List<String> symbols = List.of("SPY", "QQQ", "NVDA", "AAPL", "TSLA", "MSFT", "GOOGL", "CDR.WA", "PKO.WA", "PKN.WA");
        List<Map<String, Object>> items = new ArrayList<>();
        
        for (String sym : symbols) {
            try {
                BigDecimal price = priceService.getCurrentPrice(sym);
                List<BacktestResult.OHLCVBar> bars = priceService.fetchOHLCV(sym, "5d", "1d");
                double changePct = 0.0;
                if (bars != null && bars.size() >= 2) {
                    double prev = bars.get(bars.size() - 2).close;
                    double current = price.doubleValue();
                    if (prev > 0) changePct = ((current - prev) / prev) * 100.0;
                }
                
                String displaySym = sym.replace(".WA", "");
                String label = displaySym.equals("SPY") ? "S&P 500" : (displaySym.equals("QQQ") ? "NASDAQ 100" : displaySym);
                String currency = sym.endsWith(".WA") ? "PLN" : "$";
                
                Map<String, Object> item = new HashMap<>();
                item.put("symbol", displaySym);
                item.put("label", label);
                item.put("price", String.format(currency.equals("$") ? "$%.2f" : "%.2f PLN", price.doubleValue()));
                item.put("changePct", Math.round(changePct * 100.0) / 100.0);
                items.add(item);
            } catch (Exception e) {
                // Ignore fallback for individual failed symbol
            }
        }
        return Map.of("success", true, "items", items);
    }

    @PostMapping("/order")
    @ResponseBody
    public Map<String, Object> executeOrder(@AuthenticationPrincipal User user,
                                            @RequestParam String side,
                                            @RequestParam String symbol,
                                            @RequestParam BigDecimal shares) {
        if (user == null) {
            return Map.of("success", false, "message", "Musisz być zalogowany.");
        }

        try {
            SimulatedTrade.Side tradeSide = SimulatedTrade.Side.valueOf(side.toUpperCase());
            simulatorService.executeTrade(user.getId(), tradeSide, symbol, shares);

            SimulatedWallet wallet = simulatorService.getOrCreateWallet(user.getId());
            return Map.of(
                "success", true,
                "message", "Zleczenie " + (tradeSide == SimulatedTrade.Side.BUY ? "zakupu" : "sprzedaży") + " zostało pomyślnie zrealizowane!",
                "newBalance", wallet.getBalance()
            );
        } catch (Exception e) {
            return Map.of("success", false, "message", e.getMessage() != null ? e.getMessage() : "Błąd realizacji zlecenia.");
        }
    }
}
