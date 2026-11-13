package com.example.scheduler_service.controller;

import com.example.scheduler_service.dto.BaseResponse;
import com.example.scheduler_service.service.ImportSchedulerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/scheduler")
@RequiredArgsConstructor
public class SchedulerController {

    private final ImportSchedulerService importSchedulerService;

    @PostMapping("/import")
    public BaseResponse<String> manualImport() {
        String result = importSchedulerService.triggerManualImport();
        return BaseResponse.ok("Pemicu impor manual selesai", result);
    }
}
