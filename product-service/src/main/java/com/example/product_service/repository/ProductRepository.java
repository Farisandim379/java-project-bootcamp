package com.example.product_service.repository;

import com.example.product_service.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByNamaContainingIgnoreCase(String huruf);

    boolean existsByNamaIgnoreCaseAndBrandIgnoreCase(String nama, String brand);

    boolean existsByDummyJsonId(Long dummyJsonId);

    Optional<Product> findByDummyJsonId(Long dummyJsonId);

    List<Product> findByCreatedAtBetweenOrderByIdAsc(LocalDateTime dari, LocalDateTime sampai);
}
