package com.bootcamp19.helloworld.service;

import com.bootcamp19.helloworld.dto.feign.QuoteResponse;

import java.util.concurrent.CompletableFuture;

public interface QuoteAsyncService {
    CompletableFuture<QuoteResponse> getQuote();
}
