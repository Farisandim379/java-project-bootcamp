package com.bootcamp19.helloworld.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuotePageAnalysisResponse {
    private int total;
    private int skip;
    private int limit;
    private List<CharAnalysisResponse> hasil;
}
