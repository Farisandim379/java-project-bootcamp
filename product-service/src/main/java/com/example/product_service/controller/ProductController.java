package com.example.product_service.controller;

import com.example.product_service.dto.request.ProductCreateRequest;
import com.example.product_service.dto.request.ProductUpdateRequest;
import com.example.product_service.dto.response.BaseResponse;
import com.example.product_service.dto.response.ProductResponse;
import com.example.product_service.dto.response.ReportResponse;
import com.example.product_service.service.ProductService;
import com.example.product_service.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ReportService reportService;

    @PostMapping
    public ResponseEntity<BaseResponse<ProductResponse>> createProduct(
            @Valid @RequestBody ProductCreateRequest request,
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "anonymous") String userId
    ) {
        ProductResponse response = productService.createProduct(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.ok("Produk berhasil dibuat", response));
    }

    @GetMapping("/{id}")
    public BaseResponse<ProductResponse> getProductById(@PathVariable Long id) {
        return BaseResponse.ok(productService.getProductById(id));
    }

    @GetMapping
    public BaseResponse<List<ProductResponse>> getAllProducts(
            @RequestParam(value = "huruf", required = false) String huruf
    ) {
        return BaseResponse.ok(productService.getAllProducts(huruf));
    }

    @PutMapping("/{id}")
    public BaseResponse<ProductResponse> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductUpdateRequest request
    ) {
        return BaseResponse.ok("Produk berhasil diperbarui", productService.updateProduct(id, request));
    }

    @DeleteMapping("/{id}")
    public BaseResponse<String> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return BaseResponse.ok("Produk berhasil dihapus", null);
    }

    @PostMapping("/report")
    public BaseResponse<ReportResponse> generateReport() {
        return BaseResponse.ok("Laporan CSV berhasil dibuat", reportService.generateCsvReport());
    }
}
