package com.example.scheduler_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductImportItemDto {
    private Long id;
    private String title;
    private String brand;
    private Double price;
    private String category;
    private Double rating;
}
