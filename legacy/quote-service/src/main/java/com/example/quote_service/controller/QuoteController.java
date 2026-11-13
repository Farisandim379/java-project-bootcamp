package com.example.quote_service.controller;

import com.example.quote_service.feign.QuoteListResponse;
import com.example.quote_service.feign.QuoteResponse;
import com.example.quote_service.rest.DummyJsonClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/quotes")
@RequiredArgsConstructor
public class QuoteController {
    private final DummyJsonClient dummyJsonClient;

    @Value("${server.port}")
    private String port;

    @GetMapping("/random")
    public QuoteResponse randomQuote() {
        log.debug("Fetching random quote on port {}", port);
        return dummyJsonClient.getQuote();
    }

    @GetMapping("/{id}")
    public QuoteResponse byId(@PathVariable Long id) {
        return dummyJsonClient.getQuoteById(id);
    }

    @GetMapping("/random/{length}")
    public List<QuoteResponse> randomLength(@PathVariable int length) {
        return dummyJsonClient.getRandomQuotes(length);
    }

    @GetMapping
    public QuoteListResponse daftar(
            @RequestParam(value = "limit", defaultValue = "5") int limit,
            @RequestParam(value = "skip", defaultValue = "0") int skip) {
        return dummyJsonClient.getQuotes(limit, skip);
    }
}
