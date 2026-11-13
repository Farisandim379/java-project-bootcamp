package com.example.product_service.service;

import com.example.product_service.dto.request.ProductCreateRequest;
import com.example.product_service.dto.request.ProductImportItemDto;
import com.example.product_service.dto.request.ProductUpdateRequest;
import com.example.product_service.dto.response.ProductResponse;

import java.util.List;

public interface ProductService {
    ProductResponse createProduct(ProductCreateRequest request, String createdBy);
    ProductResponse getProductById(Long id);
    List<ProductResponse> getAllProducts(String huruf);
    ProductResponse updateProduct(Long id, ProductUpdateRequest request);
    void deleteProduct(Long id);
    int importProducts(List<ProductImportItemDto> items);
}
