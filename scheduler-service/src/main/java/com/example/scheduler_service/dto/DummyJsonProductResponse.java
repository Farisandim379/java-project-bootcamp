package com.example.scheduler_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DummyJsonProductResponse {
    private List<ProductImportItemDto> products;
    private int total;
    private int skip;
    private int limit;
}
