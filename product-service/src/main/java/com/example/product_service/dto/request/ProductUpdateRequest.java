package com.example.product_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductUpdateRequest {

    @NotBlank(message = "Nama produk wajib diisi")
    private String nama;

    @NotBlank(message = "Brand wajib diisi")
    private String brand;

    @NotNull(message = "Harga wajib diisi")
    @Positive(message = "Harga harus bernilai lebih dari 0")
    private Double harga;

    @NotBlank(message = "Kategori wajib diisi")
    private String kategori;

    private Double rating;
}
