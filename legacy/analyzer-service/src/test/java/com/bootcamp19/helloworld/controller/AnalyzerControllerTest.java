package com.bootcamp19.helloworld.controller;

import com.bootcamp19.helloworld.dto.response.CharAnalysisResponse;
import com.bootcamp19.helloworld.dto.response.QuotePageAnalysisResponse;
import com.bootcamp19.helloworld.dto.response.TrendingResponse;
import com.bootcamp19.helloworld.exception.GlobalExceptionHandler;
import com.bootcamp19.helloworld.exception.InvalidPageException;
import com.bootcamp19.helloworld.exception.QuoteNotFoundException;
import com.bootcamp19.helloworld.service.CharAnalysisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AnalyzerControllerTest {

    @Mock
    private CharAnalysisService charAnalysisService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AnalyzerController(charAnalysisService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void fromQuoteById_returnsOk() throws Exception {
        CharAnalysisResponse response = new CharAnalysisResponse(
                1L, "Halo dunia", "Anon", 9, Map.of('a', 2)
        );
        when(charAnalysisService.analysisQuoteById(5L)).thenReturn(response);

        mockMvc.perform(post("/api/v1/from-quote/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.kalimat").value("Halo dunia"));
    }

    @Test
    void fromQuoteById_whenNotFound_returns404() throws Exception {
        when(charAnalysisService.analysisQuoteById(999L)).thenThrow(new QuoteNotFoundException(999L));

        mockMvc.perform(post("/api/v1/from-quote/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("failed"))
                .andExpect(jsonPath("$.message").value("Quote dengan id 999 tidak ditemukan"));
    }

    @Test
    void fromHalamanQuote_usesDefaultsAndReturnsPageResponse() throws Exception {
        CharAnalysisResponse item = new CharAnalysisResponse(
                2L, "Test", "Author", 4, Map.of('t', 2)
        );
        QuotePageAnalysisResponse page = new QuotePageAnalysisResponse(100, 0, 5, List.of(item));
        when(charAnalysisService.analisisHalamanQuote(5, 0)).thenReturn(page);

        mockMvc.perform(post("/api/v1/from-quote/page"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.total").value(100))
                .andExpect(jsonPath("$.data.limit").value(5))
                .andExpect(jsonPath("$.data.skip").value(0))
                .andExpect(jsonPath("$.data.hasil[0].id").value(2));
    }

    @Test
    void fromHalamanQuote_invalidPage_returns400() throws Exception {
        when(charAnalysisService.analisisHalamanQuote(50, 0))
                .thenThrow(new InvalidPageException("Limit harus 1 sampai 20"));

        mockMvc.perform(post("/api/v1/from-quote/page").param("limit", "50").param("skip", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("failed"))
                .andExpect(jsonPath("$.message").value("Limit harus 1 sampai 20"));
    }

    @Test
    void trending_returnsOk() throws Exception {
        when(charAnalysisService.trending(5)).thenReturn(List.of(
                new TrendingResponse("a", 15L),
                new TrendingResponse("e", 10L)
        ));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/trending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data[0].huruf").value("a"))
                .andExpect(jsonPath("$.data[0].jumlah").value(15));
    }
}
