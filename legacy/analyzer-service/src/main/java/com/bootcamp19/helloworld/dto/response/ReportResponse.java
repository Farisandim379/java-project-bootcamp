package com.bootcamp19.helloworld.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportResponse {
    private String namaFile;
    private int totalData;
    private String linkDownload;
}
