package com.bootcamp19.helloworld.service.impl;

import com.bootcamp19.helloworld.service.RiwayatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RiwayatServiceImpl implements RiwayatService {

    @Async
    @Override
    public void catatRiwayat(Long id, String kalimat) {
        log.info("[{}] mulai catat riwayat id = {}", Thread.currentThread().getName(), id);
        try {
            Thread.sleep(10000);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        }
        log.info("[{}] selesai catat riwayat id = {}", Thread.currentThread().getName(), id);
    }
}
