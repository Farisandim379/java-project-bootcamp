package com.example.product_service.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${kafka.topic.product-created:product-created}")
    private String topicProductCreated;

    public void publishProductCreated(Long productId, String nama, String brand, Double harga) {
        String payload = String.format("{\"id\":%d,\"nama\":\"%s\",\"brand\":\"%s\",\"harga\":%.2f}",
                productId, nama, brand, harga);
        kafkaTemplate.send(topicProductCreated, String.valueOf(productId), payload)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[Kafka] Gagal mengirim event product-created: {}", ex.getMessage());
                    } else {
                        log.info("[Kafka] Event product-created terkirim: key={}, partition={}",
                                productId, result.getRecordMetadata().partition());
                    }
                });
    }
}
