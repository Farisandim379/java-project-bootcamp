package com.bootcamp19.helloworld.service.impl;

import com.bootcamp19.helloworld.consumer.AnalysisCreatedConsumer;
import com.bootcamp19.helloworld.dto.feign.QuoteListResponse;
import com.bootcamp19.helloworld.dto.feign.QuoteResponse;
import com.bootcamp19.helloworld.dto.response.CharAnalysisResponse;
import com.bootcamp19.helloworld.dto.response.QuotePageAnalysisResponse;
import com.bootcamp19.helloworld.dto.response.TrendingResponse;
import com.bootcamp19.helloworld.entity.CharAnalysis;
import com.bootcamp19.helloworld.exception.InvalidJumlahException;
import com.bootcamp19.helloworld.exception.InvalidPageException;
import com.bootcamp19.helloworld.exception.QuoteNotFoundException;
import com.bootcamp19.helloworld.producer.KafkaProducer;
import com.bootcamp19.helloworld.repository.CharAnalysisRepository;
import com.bootcamp19.helloworld.rest.QuoteClient;
import com.bootcamp19.helloworld.service.QuoteAsyncService;
import com.bootcamp19.helloworld.service.RiwayatService;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CharAnalysisServiceImplTest {

    @Mock
    private QuoteClient quoteClient;

    @Mock
    private CharAnalysisRepository charAnalysisRepository;

    @Mock
    private RiwayatService riwayatService;

    @Mock
    private QuoteAsyncService quoteAsyncService;

    @Mock
    private KafkaProducer kafkaProducer;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    @InjectMocks
    private CharAnalysisServiceImpl service;

    @Test
    void analysisQuoteById_success_savesAndReturnsResponse() {
        QuoteResponse quote = new QuoteResponse();
        quote.setId(7L);
        quote.setQuote("Stay hungry, stay foolish.");
        quote.setAuthor("Steve Jobs");

        when(quoteClient.getQuoteById(7L)).thenReturn(quote);
        when(charAnalysisRepository.existsByKalimatIgnoreCase("Stay hungry, stay foolish.")).thenReturn(false);
        when(charAnalysisRepository.save(any(CharAnalysis.class))).thenAnswer(invocation -> {
            CharAnalysis entity = invocation.getArgument(0);
            entity.setId(1L);
            return entity;
        });

        CharAnalysisResponse response = service.analysisQuoteById(7L);

        assertEquals(1L, response.getId());
        assertEquals("Stay hungry, stay foolish.", response.getKalimat());
        assertEquals("Steve Jobs", response.getAuthor());
        verify(riwayatService).catatRiwayat(1L, "Stay hungry, stay foolish.");
    }

    @Test
    void analysisQuoteById_whenNotFound_throwsQuoteNotFoundException() {
        Request request = Request.create(Request.HttpMethod.GET, "/quotes/999", Map.of(), null, StandardCharsets.UTF_8, new RequestTemplate());
        when(quoteClient.getQuoteById(999L)).thenThrow(new FeignException.NotFound("Not Found", request, null, Map.of()));

        assertThrows(QuoteNotFoundException.class, () -> service.analysisQuoteById(999L));
    }

    @Test
    void analisisHalamanQuote_success_returnsPagedAnalysis() {
        QuoteResponse quote = new QuoteResponse();
        quote.setId(1L);
        quote.setQuote("Knowledge is power.");
        quote.setAuthor("Francis Bacon");

        QuoteListResponse pageResponse = new QuoteListResponse();
        pageResponse.setTotal(100);
        pageResponse.setSkip(0);
        pageResponse.setLimit(5);
        pageResponse.setQuotes(List.of(quote));

        when(quoteClient.getQuotes(5, 0)).thenReturn(pageResponse);
        when(charAnalysisRepository.existsByKalimatIgnoreCase("Knowledge is power.")).thenReturn(false);
        when(charAnalysisRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<CharAnalysis> list = invocation.getArgument(0);
            list.get(0).setId(10L);
            return list;
        });

        QuotePageAnalysisResponse result = service.analisisHalamanQuote(5, 0);

        assertEquals(100, result.getTotal());
        assertEquals(0, result.getSkip());
        assertEquals(5, result.getLimit());
        assertEquals(1, result.getHasil().size());
        assertEquals(10L, result.getHasil().get(0).getId());
        verify(riwayatService).catatRiwayat(10L, "Knowledge is power.");
    }

    @Test
    void analisisHalamanQuote_invalidParams_throwsInvalidPageException() {
        assertThrows(InvalidPageException.class, () -> service.analisisHalamanQuote(0, 0));
        assertThrows(InvalidPageException.class, () -> service.analisisHalamanQuote(21, 0));
        assertThrows(InvalidPageException.class, () -> service.analisisHalamanQuote(5, -1));
    }

    @Test
    void analisisBatchDariQuote_invalidJumlah_throwsInvalidJumlahException() {
        assertThrows(InvalidJumlahException.class, () -> service.analisisBatchDariQuote(0));
        assertThrows(InvalidJumlahException.class, () -> service.analisisBatchDariQuote(11));
    }

    @Test
    void trending_returnsTopScores() {
        Set<ZSetOperations.TypedTuple<String>> tuples = new LinkedHashSet<>();
        tuples.add(new DefaultTypedTuple<>("a", 15.0));
        tuples.add(new DefaultTypedTuple<>("e", 10.0));

        when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.reverseRangeWithScores(AnalysisCreatedConsumer.TRENDING_KEY, 0, 4L)).thenReturn(tuples);

        List<TrendingResponse> result = service.trending(5);

        assertEquals(2, result.size());
        assertEquals("a", result.get(0).getHuruf());
        assertEquals(15L, result.get(0).getJumlah());
        assertEquals("e", result.get(1).getHuruf());
        assertEquals(10L, result.get(1).getJumlah());
    }
}
