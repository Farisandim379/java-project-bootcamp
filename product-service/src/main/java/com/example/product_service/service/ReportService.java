package com.example.product_service.service;

import com.example.product_service.dto.response.ReportResponse;

public interface ReportService {
    ReportResponse generateCsvReport();
    String getDownloadLink(String namaFile);
}
