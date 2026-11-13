package com.bootcamp19.helloworld.service;

import com.bootcamp19.helloworld.dto.request.CharAnalysisRequest;
import com.bootcamp19.helloworld.dto.response.CharAnalysisResponse;
import com.bootcamp19.helloworld.dto.response.QuotePageAnalysisResponse;
import com.bootcamp19.helloworld.dto.response.TrendingResponse;

import java.util.List;

public interface CharAnalysisService {
    List<CharAnalysisResponse> cari(String kata);

    CharAnalysisResponse getById(Long id);

    CharAnalysisResponse analisis(CharAnalysisRequest request);

    List<CharAnalysisResponse> getMinimal(int min);

    List<CharAnalysisResponse> getTerbaru();

    CharAnalysisResponse analisisDariQuote();

    CharAnalysisResponse analysisQuoteById(Long id);

    List<CharAnalysisResponse> analisisBatchDariQuote(int jumlah);

    QuotePageAnalysisResponse analisisHalamanQuote(int limit, int skip);

    List<CharAnalysisResponse> analysisDoubleQuote();

    List<TrendingResponse> trending(int limit);
}
