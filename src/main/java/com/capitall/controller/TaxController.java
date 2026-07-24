package com.capitall.controller;

import com.capitall.dto.TaxReportDto;
import com.capitall.model.User;
import com.capitall.service.TaxReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/tax")
@PreAuthorize("isAuthenticated()")
public class TaxController {

    private final TaxReportService taxReportService;

    public TaxController(TaxReportService taxReportService) {
        this.taxReportService = taxReportService;
    }

    @GetMapping("/report")
    public ResponseEntity<TaxReportDto> getTaxReport(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "2026") int year,
            @RequestParam(defaultValue = "FIFO") String method) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(taxReportService.generateReport(user.getId(), year, method));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportTaxReportCsv(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "2026") int year,
            @RequestParam(defaultValue = "FIFO") String method) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        TaxReportDto report = taxReportService.generateReport(user.getId(), year, method);
        byte[] csvData = taxReportService.exportReportToCsv(report);

        String filename = String.format("tax_report_%d_%s.csv", year, method.toUpperCase());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvData);
    }
}
