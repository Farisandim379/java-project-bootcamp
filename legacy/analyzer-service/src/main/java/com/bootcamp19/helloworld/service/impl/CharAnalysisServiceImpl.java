package com.bootcamp19.helloworld.service.impl;

import com.bootcamp19.helloworld.dto.feign.QuoteListResponse;
import com.bootcamp19.helloworld.dto.feign.QuoteResponse;
import com.bootcamp19.helloworld.dto.request.CharAnalysisRequest;
import com.bootcamp19.helloworld.dto.response.CharAnalysisResponse;
import com.bootcamp19.helloworld.dto.response.QuotePageAnalysisResponse;
import com.bootcamp19.helloworld.entity.CharAnalysis;
import com.bootcamp19.helloworld.exception.AnalysisConflictException;
import com.bootcamp19.helloworld.exception.AnalysisNotFoundException;
import com.bootcamp19.helloworld.exception.InvalidJumlahException;
import com.bootcamp19.helloworld.exception.InvalidPageException;
import com.bootcamp19.helloworld.exception.QuoteNotFoundException;
import com.bootcamp19.helloworld.exception.QuoteUnavailableException;
import com.bootcamp19.helloworld.repository.CharAnalysisRepository;
import com.bootcamp19.helloworld.rest.QuoteClient;
import com.bootcamp19.helloworld.service.CharAnalysisService;
import com.bootcamp19.helloworld.service.QuoteAsyncService;
import com.bootcamp19.helloworld.service.RiwayatService;
import com.bootcamp19.helloworld.consumer.AnalysisCreatedConsumer;
import com.bootcamp19.helloworld.dto.response.TrendingResponse;
import com.bootcamp19.helloworld.producer.KafkaProducer;
import com.bootcamp19.helloworld.util.CharCounter;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Service
@RequiredArgsConstructor
@Slf4j
public class CharAnalysisServiceImpl implements CharAnalysisService {

    private static final int MAX_KALIMAT_LENGTH = 500;
    private static final int MAX_AUTHOR_LENGTH = 100;

    private final QuoteClient client;
    private final CharAnalysisRepository charAnalysisRepository;
    private final RiwayatService riwayatService;
    private final QuoteAsyncService quoteAsyncService;
    private final KafkaProducer kafkaProducer;
    private final StringRedisTemplate stringRedisTemplate;

    @Value("${kafka.topic.analysis-created}")
    private String topic;

    @Override
    @Transactional
    public List<CharAnalysisResponse> analysisDoubleQuote() {
        long mulai = System.currentTimeMillis();

        CompletableFuture<QuoteResponse> quote1 = quoteAsyncService.getQuote();
        CompletableFuture<QuoteResponse> quote2 = quoteAsyncService.getQuote();

        QuoteResponse hasilQuote1;
        QuoteResponse hasilQuote2;
        try {
            hasilQuote1 = quote1.join();
            hasilQuote2 = quote2.join();
        } catch (CompletionException completionException) {
            throw new QuoteUnavailableException("Service unavailable");
        }

        List<CharAnalysisResponse> hasil = saveQuotesBatch(List.of(
                hasilQuote1 != null ? hasilQuote1 : new QuoteResponse(),
                hasilQuote2 != null ? hasilQuote2 : new QuoteResponse()
        ));
        log.info("[{}] Dua quote selesai dalam {} ms",
                Thread.currentThread().getName(), System.currentTimeMillis() - mulai);
        return hasil;
    }

    @Override
    @CacheEvict(value = "analysis", allEntries = true)
    public CharAnalysisResponse analisis(CharAnalysisRequest request) {
        String kalimat = request != null && request.getKalimat() != null ? request.getKalimat().trim() : "";
        return simpan(kalimat, null);
    }

    @Override
    public List<CharAnalysisResponse> cari(String kata) {
        List<CharAnalysis> semua;
        if (kata == null || kata.isBlank()) {
            semua = charAnalysisRepository.findAll();
        } else {
            semua = charAnalysisRepository.findByKalimatContainingIgnoreCase(kata);
        }

        List<CharAnalysisResponse> hasil = new ArrayList<>();
        for (CharAnalysis data : semua) {
            hasil.add(toResponse(data));
        }
        return hasil;
    }

    @Override
    @Cacheable(value = "analysis", key="#id")
    public CharAnalysisResponse getById(Long id) {
        CharAnalysis data = charAnalysisRepository.findById(id)
                .orElseThrow(() -> new AnalysisNotFoundException(id));
        return toResponse(data);
    }

    @Override
    public List<CharAnalysisResponse> getMinimal(int min) {
        List<CharAnalysis> list = charAnalysisRepository.findByMinimalHuruf(min);
        List<CharAnalysisResponse> hasil = new ArrayList<>();
        for (CharAnalysis data : list) {
            hasil.add(toResponse(data));
        }
        return hasil;
    }

    @Override
    public List<CharAnalysisResponse> getTerbaru() {
        List<CharAnalysis> list = charAnalysisRepository.findTop5Terbaru();
        List<CharAnalysisResponse> hasil = new ArrayList<>();
        for (CharAnalysis data : list) {
            hasil.add(toResponse(data));
        }
        return hasil;
    }

    @Override
    public CharAnalysisResponse analisisDariQuote() {
        QuoteResponse quote;
        try {
            quote = client.getQuote();
        } catch (FeignException feignException) {
            log.error("Feign ke quote-service gagal: status={}, pesan={}",
                    feignException.status(), feignException.getMessage());
            throw new QuoteUnavailableException("Service unavailable");
        }

        if (quote == null || quote.getQuote() == null || quote.getQuote().isBlank()) {
            throw new QuoteUnavailableException("Quote data is empty");
        }

        return simpan(quote.getQuote().trim(), quote.getAuthor());
    }

    @Override
    public CharAnalysisResponse analysisQuoteById(Long id) {
        QuoteResponse quoteResponse;
        try {
            quoteResponse = client.getQuoteById(id);
        } catch (FeignException.NotFound feignException) {
            throw new QuoteNotFoundException(id);
        } catch (FeignException feignException) {
            throw new QuoteUnavailableException("Service unavailable");
        }

        if (quoteResponse == null || quoteResponse.getQuote() == null || quoteResponse.getQuote().isBlank()) {
            throw new QuoteUnavailableException("Quote data is empty");
        }

        log.info("Quote id={}: {} - {}", id, quoteResponse.getQuote(), quoteResponse.getAuthor());
        return simpan(quoteResponse.getQuote().trim(), quoteResponse.getAuthor());
    }

    @Override
    @Transactional
    public List<CharAnalysisResponse> analisisBatchDariQuote(int jumlah) {
        if (jumlah < 1 || jumlah > 10) {
            throw new InvalidJumlahException();
        }

        List<QuoteResponse> quotes;
        try {
            quotes = client.getQuotesBatch(jumlah);
        } catch (FeignException feignException) {
            throw new QuoteUnavailableException("Service unavailable");
        }

        if (quotes == null || quotes.isEmpty()) {
            throw new QuoteUnavailableException("Quote data is empty");
        }

        return saveQuotesBatch(quotes);
    }

    @Override
    @Transactional
    public QuotePageAnalysisResponse analisisHalamanQuote(int limit, int skip) {
        if (limit < 1 || limit > 20) {
            throw new InvalidPageException("Limit harus 1 sampai 20");
        }
        if (skip < 0) {
            throw new InvalidPageException("Skip tidak boleh negatif");
        }

        QuoteListResponse halaman;
        try {
            halaman = client.getQuotes(limit, skip);
        } catch (FeignException feignException) {
            throw new QuoteUnavailableException("Service unavailable");
        }

        if (halaman == null || halaman.getQuotes() == null || halaman.getQuotes().isEmpty()) {
            throw new QuoteUnavailableException("Quote data is empty");
        }

        List<CharAnalysisResponse> hasil = saveQuotesBatch(halaman.getQuotes());
        log.info("Halaman skip={} limit={}: {} quote disimpan", skip, limit, hasil.size());

        return new QuotePageAnalysisResponse(halaman.getTotal(), halaman.getSkip(), halaman.getLimit(), hasil);
    }

    private List<CharAnalysisResponse> saveQuotesBatch(List<QuoteResponse> quotes) {
        List<CharAnalysis> entitiesToSave = new ArrayList<>();
        Set<String> seenInBatch = new HashSet<>();
        boolean hasNonBlankQuote = false;

        for (QuoteResponse quote : quotes) {
            if (quote == null || quote.getQuote() == null || quote.getQuote().isBlank()) {
                continue;
            }
            hasNonBlankQuote = true;
            String kalimat = quote.getQuote().trim();
            if (kalimat.length() > MAX_KALIMAT_LENGTH) {
                continue;
            }
            String key = kalimat.toLowerCase();

            if (seenInBatch.contains(key) || charAnalysisRepository.existsByKalimatIgnoreCase(kalimat)) {
                continue;
            }
            seenInBatch.add(key);
            entitiesToSave.add(buildEntity(kalimat, quote.getAuthor(), new CharCounter(kalimat)));
        }

        if (!hasNonBlankQuote) {
            throw new QuoteUnavailableException("Quote data is empty");
        }

        if (entitiesToSave.isEmpty()) {
            throw new AnalysisConflictException("Semua quote yang diambil sudah ada di database atau melebihi batas panjang");
        }

        List<CharAnalysis> savedList = charAnalysisRepository.saveAll(entitiesToSave);
        List<CharAnalysisResponse> hasil = new ArrayList<>();
        for (CharAnalysis saved : savedList) {
            riwayatService.catatRiwayat(saved.getId(), saved.getKalimat());
            kafkaProducer.sendMessage(topic, saved.getKalimat());
            hasil.add(toResponse(saved));
        }
        return hasil;
    }

    @Override
    public List<TrendingResponse> trending(int limit) {
        Set<ZSetOperations.TypedTuple<String>> dataSet = stringRedisTemplate.opsForZSet()
                .reverseRangeWithScores(AnalysisCreatedConsumer.TRENDING_KEY, 0, limit - 1L);

        List<TrendingResponse> results = new ArrayList<>();
        if (dataSet != null) {
            for (ZSetOperations.TypedTuple<String> data : dataSet) {
                long score = data.getScore() == null ? 0L : data.getScore().longValue();
                results.add(new TrendingResponse(data.getValue(), score));
            }
        }
        return results;
    }

    private CharAnalysisResponse simpan(String kalimat, String author) {
        if (kalimat.isBlank()) {
            throw new IllegalArgumentException("Kalimat wajib di isi");
        }
        if (kalimat.length() > MAX_KALIMAT_LENGTH) {
            throw new IllegalArgumentException("Kalimat maksimal " + MAX_KALIMAT_LENGTH + " character");
        }
        if (charAnalysisRepository.existsByKalimatIgnoreCase(kalimat)) {
            throw new AnalysisConflictException(kalimat);
        }

        CharCounter counter = new CharCounter(kalimat);
        CharAnalysis entity = buildEntity(kalimat, author, counter);

        CharAnalysis saved = charAnalysisRepository.save(entity);
        kafkaProducer.sendMessage(topic, kalimat);
        riwayatService.catatRiwayat(saved.getId(), kalimat);
        log.info("[{}] selesai simpan id = {}", Thread.currentThread().getName(), saved.getId());

        return CharAnalysisResponse.from(saved, counter.toMap());
    }

    private CharAnalysis buildEntity(String kalimat, String author, CharCounter counter) {
        String normalizedAuthor = author != null ? author.trim() : null;
        if (normalizedAuthor != null && normalizedAuthor.length() > MAX_AUTHOR_LENGTH) {
            normalizedAuthor = normalizedAuthor.substring(0, MAX_AUTHOR_LENGTH);
        }
        CharAnalysis entity = new CharAnalysis();
        entity.setKalimat(kalimat);
        entity.setAuthor(normalizedAuthor);
        entity.setTotalHuruf(counter.getTotalHuruf());
        entity.setCreatedAt(LocalDateTime.now());
        return entity;
    }

    private CharAnalysisResponse toResponse(CharAnalysis data) {
        CharCounter counter = new CharCounter(data.getKalimat());
        return CharAnalysisResponse.from(data, counter.toMap());
    }
}
