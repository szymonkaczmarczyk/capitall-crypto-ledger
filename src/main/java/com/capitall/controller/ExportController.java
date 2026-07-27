package com.capitall.controller;

import com.capitall.service.ExportService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.capitall.repository.UserRepository;

import java.util.UUID;

@RestController
@RequestMapping("/api/export")
public class ExportController {

    private final ExportService exportService;
    private final UserRepository userRepository;

    public ExportController(ExportService exportService, UserRepository userRepository) {
        this.exportService = exportService;
        this.userRepository = userRepository;
    }

    private UUID getUserId(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername()).orElseThrow().getId();
    }

    private ResponseEntity<ByteArrayResource> downloadCsv(String content, String filename) {
        ByteArrayResource resource = new ByteArrayResource(content.getBytes());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType("text/csv"))
                .contentLength(resource.contentLength())
                .body(resource);
    }

    @GetMapping("/trades")
    public ResponseEntity<ByteArrayResource> exportTrades(@AuthenticationPrincipal UserDetails userDetails) {
        String csv = exportService.exportTradesToCsv(getUserId(userDetails));
        return downloadCsv(csv, "trades_history.csv");
    }

    @GetMapping("/equity")
    public ResponseEntity<ByteArrayResource> exportEquity(@AuthenticationPrincipal UserDetails userDetails) {
        String csv = exportService.exportEquityToCsv(getUserId(userDetails));
        return downloadCsv(csv, "equity_snapshots.csv");
    }

    @GetMapping("/alerts")
    public ResponseEntity<ByteArrayResource> exportAlerts(@AuthenticationPrincipal UserDetails userDetails) {
        String csv = exportService.exportAlertLogsToCsv(getUserId(userDetails));
        return downloadCsv(csv, "alerts_history.csv");
    }
}
