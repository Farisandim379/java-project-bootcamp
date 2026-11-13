package com.bootcamp19.helloworld.service.impl;

import com.bootcamp19.helloworld.dto.feign.QuoteResponse;
import com.bootcamp19.helloworld.rest.QuoteClient;
import com.bootcamp19.helloworld.service.QuoteAsyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class QuoteAsyncServiceImpl implements QuoteAsyncService {
    private final QuoteClient quoteClient;

    @Async
    @Override
    public CompletableFuture<QuoteResponse> getQuote() {
        log.info("[{}] Mulai ambil quote async", Thread.currentThread().getName());
        QuoteResponse quoteResponse = quoteClient.getQuote();
        log.info("[{}] Selesai ambil quote async id = {}",
                Thread.currentThread().getName(),
                quoteResponse != null ? quoteResponse.getId() : null);
        return CompletableFuture.completedFuture(quoteResponse);
    }
}
