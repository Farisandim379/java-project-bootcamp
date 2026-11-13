package com.example.product_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse implements Serializable {
    private Long id;
    private String nama;
    private String brand;
    private Double harga;
    private String kategori;
    private Double rating;
    private Long dummyJsonId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
