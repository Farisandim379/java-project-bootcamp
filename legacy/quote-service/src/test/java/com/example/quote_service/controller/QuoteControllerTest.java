package com.example.quote_service.controller;

import com.example.quote_service.exception.FeignErrorHandler;
import com.example.quote_service.feign.QuoteListResponse;
import com.example.quote_service.feign.QuoteResponse;
import com.example.quote_service.rest.DummyJsonClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class QuoteControllerTest {

    @Mock
    private DummyJsonClient dummyJsonClient;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new QuoteController(dummyJsonClient))
                .setControllerAdvice(new FeignErrorHandler())
                .build();
    }

    @Test
    void daftar_returnsPagedQuotes() throws Exception {
        QuoteResponse q = new QuoteResponse();
        q.setId(1L);
        q.setQuote("Simple quote");
        q.setAuthor("Author");

        QuoteListResponse listResponse = new QuoteListResponse();
        listResponse.setTotal(100);
        listResponse.setSkip(0);
        listResponse.setLimit(5);
        listResponse.setQuotes(List.of(q));

        when(dummyJsonClient.getQuotes(5, 0)).thenReturn(listResponse);

        mockMvc.perform(get("/quotes").param("limit", "5").param("skip", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(100))
                .andExpect(jsonPath("$.limit").value(5))
                .andExpect(jsonPath("$.skip").value(0))
                .andExpect(jsonPath("$.quotes[0].quote").value("Simple quote"));
    }
}
