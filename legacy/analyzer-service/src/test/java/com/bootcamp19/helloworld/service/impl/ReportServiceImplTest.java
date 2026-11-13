package com.bootcamp19.helloworld.service.impl;

import com.bootcamp19.helloworld.dto.response.ReportResponse;
import com.bootcamp19.helloworld.entity.CharAnalysis;
import com.bootcamp19.helloworld.repository.CharAnalysisRepository;
import com.bootcamp19.helloworld.util.MinioUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @Mock
    private CharAnalysisRepository charAnalysisRepository;

    @Mock
    private MinioUtil minioUtil;

    @InjectMocks
    private ReportServiceImpl reportService;

    @Test
    void testBuatLaporanHarian() {
        CharAnalysis sample = new CharAnalysis();
        sample.setId(1L);
        sample.setKalimat("test kalimat");
        sample.setAuthor("penulis");
        sample.setTotalHuruf(11);
        sample.setCreatedAt(LocalDateTime.now());

        when(charAnalysisRepository.findByCreatedAtBetweenOrderByIdAsc(any(), any()))
                .thenReturn(List.of(sample));
        when(minioUtil.linkDownload(anyString()))
                .thenReturn("http://localhost:9000/analyzer-report/test.csv?token=123");

        ReportResponse response = reportService.buatLaporanHarian();

        assertNotNull(response);
        assertEquals(1, response.getTotalData());
        assertTrue(response.getNamaFile().startsWith("report-"));
        assertTrue(response.getNamaFile().endsWith(".csv"));
        assertEquals("http://localhost:9000/analyzer-report/test.csv?token=123", response.getLinkDownload());

        verify(minioUtil, times(1)).upload(anyString(), any(byte[].class), eq("text/csv"));
        verify(minioUtil, times(1)).linkDownload(anyString());
    }

    @Test
    void testLinkDownload() {
        when(minioUtil.linkDownload("report-123.csv"))
                .thenReturn("http://localhost:9000/analyzer-report/report-123.csv?token=xyz");

        String url = reportService.linkDownload("report-123.csv");
        assertEquals("http://localhost:9000/analyzer-report/report-123.csv?token=xyz", url);
        verify(minioUtil, times(1)).linkDownload("report-123.csv");
    }
}
