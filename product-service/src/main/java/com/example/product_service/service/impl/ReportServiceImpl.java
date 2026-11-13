package com.example.product_service.service.impl;

import com.example.product_service.dto.response.ReportResponse;
import com.example.product_service.entity.Product;
import com.example.product_service.repository.ProductRepository;
import com.example.product_service.service.ReportService;
import com.example.product_service.util.MinioUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ProductRepository productRepository;
    private final MinioUtil minioUtil;

    @Override
    public ReportResponse generateCsvReport() {
        LocalDateTime sampai = LocalDateTime.now();
        LocalDateTime dari = sampai.minusDays(7); // Laporan 7 hari terakhir
        List<Product> products = productRepository.findByCreatedAtBetweenOrderByIdAsc(dari, sampai);

        StringBuilder csv = new StringBuilder("id,nama,brand,harga,kategori,rating,created_at\n");
        for (Product p : products) {
            csv.append(p.getId()).append(',')
                    .append('"').append(p.getNama() == null ? "" : p.getNama().replace("\"", "\"\"")).append('"').append(',')
                    .append('"').append(p.getBrand() == null ? "" : p.getBrand().replace("\"", "\"\"")).append('"').append(',')
                    .append(p.getHarga()).append(',')
                    .append(p.getKategori() == null ? "" : p.getKategori()).append(',')
                    .append(p.getRating() == null ? 0.0 : p.getRating()).append(',')
                    .append(p.getCreatedAt()).append('\n');
        }

        String namaFile = "products-report-" + System.currentTimeMillis() + ".csv";
        minioUtil.upload(namaFile, csv.toString().getBytes(StandardCharsets.UTF_8), "text/csv");
        log.info("[Report] Laporan CSV {} berhasil dibuat dengan {} baris data", namaFile, products.size());

        String downloadUrl = minioUtil.linkDownload(namaFile);
        return new ReportResponse(namaFile, products.size(), downloadUrl);
    }

    @Override
    public String getDownloadLink(String namaFile) {
        return minioUtil.linkDownload(namaFile);
    }
}
