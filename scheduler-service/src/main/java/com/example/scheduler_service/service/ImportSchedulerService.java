package com.example.scheduler_service.service;

import com.example.scheduler_service.client.DummyJsonClient;
import com.example.scheduler_service.client.ProductServiceClient;
import com.example.scheduler_service.dto.BaseResponse;
import com.example.scheduler_service.dto.DummyJsonProductResponse;
import com.example.scheduler_service.dto.ProductImportItemDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImportSchedulerService {

    private final DummyJsonClient dummyJsonClient;
    private final ProductServiceClient productServiceClient;

    private final AtomicInteger currentSkip = new AtomicInteger(0);
    private static final int BATCH_SIZE = 5;

    /**
     * Impor awal saat startup aplikasi tanpa menunggu jadwal cron pertama (W5.2)
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        log.info("[Scheduler Startup] Aplikasi scheduler-service siap, memicu impor awal DummyJSON...");
        executeImport("STARTUP");
    }

    /**
     * Jadwal otomatis setiap 2 menit: 0 * /2 * * * * (W5.2)
     */
    @Scheduled(cron = "${import.scheduler.cron:0 */2 * * * *}")
    public void scheduledImport() {
        log.info("[Scheduler Cron] Menjalankan jadwal impor rutin setiap 2 menit...");
        executeImport("CRON");
    }

    /**
     * Memicu impor manual melalui HTTP request (W5.7)
     */
    public String triggerManualImport() {
        log.info("[Scheduler Manual] Memicu impor manual via endpoint...");
        return executeImport("MANUAL");
    }

    /**
     * Logika eksekusi batch impor dengan penanganan error tangguh (W5.6)
     */
    public synchronized String executeImport(String source) {
        int skip = currentSkip.get();
        log.info("[Impor Batch] Sumber: {}, mengambil kategori smartphones (skip={}, limit={}) dari DummyJSON",
                source, skip, BATCH_SIZE);

        try {
            DummyJsonProductResponse dummyResponse = dummyJsonClient.getSmartphones(BATCH_SIZE, skip);
            if (dummyResponse == null || dummyResponse.getProducts() == null || dummyResponse.getProducts().isEmpty()) {
                log.info("[Impor Batch] Tidak ada produk baru yang dikembalikan oleh DummyJSON.");
                currentSkip.set(0); // Reset ke awal jika habis
                return "Tidak ada produk baru dari DummyJSON";
            }

            List<ProductImportItemDto> products = dummyResponse.getProducts();
            log.info("[Impor Batch] Berhasil mengambil {} produk dari DummyJSON (total di API: {})",
                    products.size(), dummyResponse.getTotal());

            // Kirim ke product-service melalui Feign Client
            BaseResponse<String> result = productServiceClient.sendImportedProducts(products);
            log.info("[Impor Batch] Respon dari product-service: {}", result != null ? result.getMessage() : "null");

            // Update skip untuk putaran berikutnya (maksimal 5 produk per putaran)
            int nextSkip = skip + BATCH_SIZE;
            if (nextSkip >= dummyResponse.getTotal()) {
                log.info("[Impor Batch] Telah mencapai akhir data (total {}), mereset offset skip ke 0 untuk putaran berikutnya",
                        dummyResponse.getTotal());
                currentSkip.set(0);
            } else {
                currentSkip.set(nextSkip);
            }

            return "Berhasil memproses batch (" + products.size() + " produk, skip=" + skip + ")";
        } catch (Exception e) {
            // W5.6: Kalau DummyJSON atau product-service tidak bisa dihubungi, scheduler tidak mati
            log.error("[Impor Error] Gagal mengeksekusi impor dari DummyJSON atau menghubungi product-service: {}",
                    e.getMessage(), e);
            return "Gagal menjalankan impor: " + e.getMessage();
        }
    }
}
