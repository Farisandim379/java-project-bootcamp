package com.example.scheduler_service.client;

import com.example.scheduler_service.dto.DummyJsonProductResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "dummyJsonClient", url = "${dummyjson.url:https://dummyjson.com}")
public interface DummyJsonClient {

    @GetMapping("/products/category/smartphones")
    DummyJsonProductResponse getSmartphones(
            @RequestParam(value = "limit", defaultValue = "5") int limit,
            @RequestParam(value = "skip", defaultValue = "0") int skip
    );
}
