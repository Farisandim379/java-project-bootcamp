package com.example.product_service.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ProductEventConsumer {

    @KafkaListener(topics = "${kafka.topic.product-created:product-created}", groupId = "product-summary-group")
    public void consumeProductCreated(String message) {
        log.info("[Kafka Consumer] Menerima event Produk Baru: {}", message);
    }
}
