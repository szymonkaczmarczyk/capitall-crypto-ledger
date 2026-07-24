package com.capitall.service;

import com.capitall.dto.TaxReportDto;
import java.util.UUID;

public interface TaxReportService {
    TaxReportDto generateReport(UUID userId, int year, String method);
    byte[] exportReportToCsv(TaxReportDto report);
}
