package com.capitall.service;

import com.capitall.model.EquitySnapshot;
import com.capitall.model.PriceAlertLog;
import com.capitall.model.Trade;
import com.capitall.repository.EquitySnapshotRepository;
import com.capitall.repository.PriceAlertLogRepository;
import com.capitall.repository.TradeRepository;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.UUID;

@Service
public class ExportService {

    private final TradeRepository tradeRepository;
    private final EquitySnapshotRepository equitySnapshotRepository;
    private final PriceAlertLogRepository priceAlertLogRepository;

    public ExportService(TradeRepository tradeRepository,
                         EquitySnapshotRepository equitySnapshotRepository,
                         PriceAlertLogRepository priceAlertLogRepository) {
        this.tradeRepository = tradeRepository;
        this.equitySnapshotRepository = equitySnapshotRepository;
        this.priceAlertLogRepository = priceAlertLogRepository;
    }

    public String exportTradesToCsv(UUID userId) {
        List<Trade> trades = tradeRepository.findAll().stream().filter(t -> t.getUserId().equals(userId)).toList();
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        pw.println("Data,Symbol,Strona,Ilosc,Cena (USD),Oplata (USD),Wartosc Brutto (USD),Zrealizowany PnL (USD)");
        for (Trade t : trades) {
            pw.printf("%s,%s,%s,%s,%s,%s,%s,%s%n",
                    t.getCreatedAt(),
                    t.getSymbol(),
                    t.getSide(),
                    t.getCoinAmount(),
                    t.getPrice(),
                    t.getFee(),
                    t.getGross(),
                    t.getRealizedPnl());
        }
        return sw.toString();
    }

    public String exportEquityToCsv(UUID userId) {
        List<EquitySnapshot> snapshots = equitySnapshotRepository.findAll().stream().filter(s -> s.getUserId().equals(userId)).toList();
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        pw.println("Data,Calkowite Saldo (USD),Gotowka (USD),Kryptowaluty (USD)");
        for (EquitySnapshot s : snapshots) {
            pw.printf("%s,%s,%s,%s%n",
                    s.getCapturedAt(),
                    s.getEquityUsd(),
                    s.getCashUsd(),
                    s.getCryptoUsd());
        }
        return sw.toString();
    }

    public String exportAlertLogsToCsv(UUID userId) {
        List<PriceAlertLog> logs = priceAlertLogRepository.findAll().stream().filter(l -> l.getUserId().equals(userId)).toList();
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        pw.println("Data,Symbol,Typ,Akcja,Szczegoly");
        for (PriceAlertLog log : logs) {
            pw.printf("%s,%s,%s,%s,\"%s\"%n",
                    log.getOccurredAt(),
                    log.getSymbol(),
                    log.getAssetType(),
                    log.getAction(),
                    log.getDetails().replace("\"", "\"\"")); // Escape CSV quotes
        }
        return sw.toString();
    }
}
