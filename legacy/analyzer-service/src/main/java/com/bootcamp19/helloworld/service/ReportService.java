package com.bootcamp19.helloworld.service;

import com.bootcamp19.helloworld.dto.response.ReportResponse;

public interface ReportService {
    ReportResponse buatLaporanHarian();
    String linkDownload(String namaFile);
}
