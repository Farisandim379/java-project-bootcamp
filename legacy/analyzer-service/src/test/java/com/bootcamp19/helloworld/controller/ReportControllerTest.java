package com.bootcamp19.helloworld.controller;

import com.bootcamp19.helloworld.dto.response.ReportResponse;
import com.bootcamp19.helloworld.service.ReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportController.class)
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportService reportService;

    @Test
    void testGenerate() throws Exception {
        ReportResponse response = new ReportResponse("report-123.csv", 5, "http://download.url");
        when(reportService.buatLaporanHarian()).thenReturn(response);

        mockMvc.perform(post("/api/v1/reports/generate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.namaFile").value("report-123.csv"))
                .andExpect(jsonPath("$.data.totalData").value(5))
                .andExpect(jsonPath("$.data.linkDownload").value("http://download.url"));
    }

    @Test
    void testLink() throws Exception {
        when(reportService.linkDownload("report-123.csv")).thenReturn("http://download.url");

        mockMvc.perform(get("/api/v1/reports/report-123.csv/url"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data").value("http://download.url"));
    }
}
