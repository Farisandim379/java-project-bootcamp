package com.example.product_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(name = "dummyJsonWriteClient", url = "${dummyjson.url:https://dummyjson.com}")
public interface DummyJsonWriteClient {

    @PostMapping(value = "/products/add", headers = "User-Agent=Mozilla/5.0")
    Map<String, Object> addProduct(@RequestBody Map<String, Object> request);

    @PutMapping(value = "/products/{id}", headers = "User-Agent=Mozilla/5.0")
    Map<String, Object> updateProduct(@PathVariable("id") Long id, @RequestBody Map<String, Object> request);

    @DeleteMapping(value = "/products/{id}", headers = "User-Agent=Mozilla/5.0")
    Map<String, Object> deleteProduct(@PathVariable("id") Long id);
}
