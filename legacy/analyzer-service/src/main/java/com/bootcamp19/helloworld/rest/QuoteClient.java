package com.bootcamp19.helloworld.rest;

import com.bootcamp19.helloworld.dto.feign.QuoteListResponse;
import com.bootcamp19.helloworld.dto.feign.QuoteResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "quote-service")
public interface QuoteClient {

    @GetMapping("/quotes/random")
    QuoteResponse getQuote();

    @GetMapping("/quotes/{id}")
    QuoteResponse getQuoteById(@PathVariable("id") Long id);

    @GetMapping("/quotes/random/{length}")
    List<QuoteResponse> getQuotesBatch(@PathVariable("length") int length);

    @GetMapping("/quotes")
    QuoteListResponse getQuotes(@RequestParam("limit") int limit, @RequestParam("skip") int skip);
}
