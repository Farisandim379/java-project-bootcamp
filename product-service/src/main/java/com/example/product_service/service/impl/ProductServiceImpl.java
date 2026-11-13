package com.example.product_service.service.impl;

import com.example.product_service.dto.request.ProductCreateRequest;
import com.example.product_service.dto.request.ProductImportItemDto;
import com.example.product_service.dto.request.ProductUpdateRequest;
import com.example.product_service.dto.response.ProductResponse;
import com.example.product_service.entity.Product;
import com.example.product_service.exception.DuplicateProductException;
import com.example.product_service.exception.ProductNotFoundException;
import com.example.product_service.producer.ProductEventProducer;
import com.example.product_service.repository.ProductRepository;
import com.example.product_service.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductEventProducer productEventProducer;

    @Override
    @Transactional
    public ProductResponse createProduct(ProductCreateRequest request, String createdBy) {
        log.info("[Audit User] Produk '{}' dibuat oleh user identitas: '{}'", request.getNama(), createdBy);

        if (productRepository.existsByNamaIgnoreCaseAndBrandIgnoreCase(request.getNama(), request.getBrand())) {
            throw new DuplicateProductException("Produk dengan nama '" + request.getNama() + "' dan brand '" + request.getBrand() + "' sudah ada");
        }

        Product product = Product.builder()
                .nama(request.getNama())
                .brand(request.getBrand())
                .harga(request.getHarga())
                .kategori(request.getKategori())
                .rating(request.getRating() != null ? request.getRating() : 0.0)
                .createdAt(LocalDateTime.now())
                .build();

        Product saved = productRepository.save(product);
        log.info("[Database] Produk tersimpan dengan id={}", saved.getId());

        // Event-Driven Kafka (B2)
        productEventProducer.publishProductCreated(saved.getId(), saved.getNama(), saved.getBrand(), saved.getHarga());

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "product", key = "#id")
    public ProductResponse getProductById(Long id) {
        log.info("[Database Query] Cache MISS: Mengambil detail produk dari database PostgreSQL untuk id={}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        return toResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts(String huruf) {
        List<Product> products;
        if (huruf != null && !huruf.trim().isEmpty()) {
            products = productRepository.findByNamaContainingIgnoreCase(huruf.trim());
        } else {
            products = productRepository.findAll();
        }
        return products.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    @CacheEvict(value = "product", key = "#id")
    public ProductResponse updateProduct(Long id, ProductUpdateRequest request) {
        log.info("[Database] Mengupdate produk id={} dan membersihkan Redis cache", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        product.setNama(request.getNama());
        product.setBrand(request.getBrand());
        product.setHarga(request.getHarga());
        product.setKategori(request.getKategori());
        if (request.getRating() != null) {
            product.setRating(request.getRating());
        }
        product.setUpdatedAt(LocalDateTime.now());

        Product updated = productRepository.save(product);
        return toResponse(updated);
    }

    @Override
    @Transactional
    @CacheEvict(value = "product", key = "#id")
    public void deleteProduct(Long id) {
        log.info("[Database] Menghapus produk id={} dan membersihkan Redis cache", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        productRepository.delete(product);
    }

    @Override
    @Transactional
    public int importProducts(List<ProductImportItemDto> items) {
        if (items == null || items.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (ProductImportItemDto item : items) {
            // Aturan Bisnis W5.4: Rating < 3 tidak diimpor (dicatat di log)
            if (item.getRating() != null && item.getRating() < 3.0) {
                log.warn("[Impor Bisnis Rule] Produk ditolak karena rating < 3: '{}' (Brand: {}) Rating: {}",
                        item.getTitle(), item.getBrand(), item.getRating());
                continue;
            }

            // Cek Duplikat W5.5: Berdasarkan dummyJsonId atau kombinasi nama dan brand
            boolean duplikatById = item.getId() != null && productRepository.existsByDummyJsonId(item.getId());
            boolean duplikatByNama = productRepository.existsByNamaIgnoreCaseAndBrandIgnoreCase(item.getTitle(), item.getBrand());

            if (duplikatById || duplikatByNama) {
                log.info("[Impor Duplikat] Produk '{}' ({}) sudah pernah diimpor, skip agar tidak ganda",
                        item.getTitle(), item.getBrand());
                continue;
            }

            // Simpan produk baru
            Product product = Product.builder()
                    .nama(item.getTitle())
                    .brand(item.getBrand() != null ? item.getBrand() : "Unknown")
                    .harga(item.getPrice() != null ? item.getPrice() : 0.0)
                    .kategori(item.getCategory() != null ? item.getCategory() : "smartphones")
                    .rating(item.getRating() != null ? item.getRating() : 0.0)
                    .dummyJsonId(item.getId())
                    .createdAt(LocalDateTime.now())
                    .build();

            Product saved = productRepository.save(product);
            count++;
            log.info("[Impor Berhasil] Produk '{}' tersimpan dengan id={}", saved.getNama(), saved.getId());

            // Publikasi event Kafka (B2)
            productEventProducer.publishProductCreated(saved.getId(), saved.getNama(), saved.getBrand(), saved.getHarga());
        }

        return count;
    }

    private ProductResponse toResponse(Product p) {
        return ProductResponse.builder()
                .id(p.getId())
                .nama(p.getNama())
                .brand(p.getBrand())
                .harga(p.getHarga())
                .kategori(p.getKategori())
                .rating(p.getRating())
                .dummyJsonId(p.getDummyJsonId())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
