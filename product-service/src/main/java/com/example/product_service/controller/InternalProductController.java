package com.example.product_service.controller;

import com.example.product_service.dto.request.ProductImportItemDto;
import com.example.product_service.dto.response.BaseResponse;
import com.example.product_service.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/internal/products")
@RequiredArgsConstructor
public class InternalProductController {

    private final ProductService productService;

    @PostMapping("/import")
    public BaseResponse<String> receiveImportedProducts(@RequestBody List<ProductImportItemDto> items) {
        log.info("[Internal API] Menerima data batch impor dari scheduler-service (jumlah item: {})",
                items != null ? items.size() : 0);
        int savedCount = productService.importProducts(items);
        return BaseResponse.ok("Impor selesai diproses", "Berhasil menyimpan " + savedCount + " produk baru");
    }
}
