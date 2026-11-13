package com.example.product_service.controller;

import com.example.product_service.dto.request.ProductCreateRequest;
import com.example.product_service.dto.response.ProductResponse;
import com.example.product_service.service.ProductService;
import com.example.product_service.service.ReportService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private ReportService reportService;

    @Test
    void createProduct_Success_Returns201() throws Exception {
        ProductCreateRequest request = ProductCreateRequest.builder()
                .nama("iPhone 13")
                .brand("Apple")
                .harga(999.0)
                .kategori("smartphones")
                .rating(4.5)
                .build();

        ProductResponse response = ProductResponse.builder()
                .id(1L)
                .nama("iPhone 13")
                .brand("Apple")
                .harga(999.0)
                .kategori("smartphones")
                .rating(4.5)
                .createdAt(LocalDateTime.now())
                .build();

        when(productService.createProduct(any(ProductCreateRequest.class), anyString())).thenReturn(response);

        mockMvc.perform(post("/product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("X-User-Id", "testuser"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.nama").value("iPhone 13"))
                .andExpect(jsonPath("$.data.brand").value("Apple"));
    }

    @Test
    void createProduct_InvalidData_Returns400() throws Exception {
        // Nama kosong dan harga negatif -> W3.1
        ProductCreateRequest request = ProductCreateRequest.builder()
                .nama("")
                .brand("Apple")
                .harga(-100.0)
                .kategori("smartphones")
                .build();

        mockMvc.perform(post("/product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("failed"));
    }

    @Test
    void getProductById_Success_Returns200() throws Exception {
        ProductResponse response = ProductResponse.builder()
                .id(1L)
                .nama("iPhone 13")
                .brand("Apple")
                .harga(999.0)
                .kategori("smartphones")
                .rating(4.5)
                .build();

        when(productService.getProductById(1L)).thenReturn(response);

        mockMvc.perform(get("/product/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.id").value(1L));
    }
}
