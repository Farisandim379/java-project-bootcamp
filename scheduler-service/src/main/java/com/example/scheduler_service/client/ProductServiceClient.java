package com.example.scheduler_service.client;

import com.example.scheduler_service.dto.BaseResponse;
import com.example.scheduler_service.dto.ProductImportItemDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "product-service")
public interface ProductServiceClient {

    @PostMapping("/internal/products/import")
    BaseResponse<String> sendImportedProducts(@RequestBody List<ProductImportItemDto> items);
}
