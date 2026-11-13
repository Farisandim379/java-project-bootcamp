package com.example.quote_service.exception;

import feign.FeignException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class FeignErrorHandler {

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<String> handleFeignException(FeignException feignException) {
        HttpStatus status = HttpStatus.resolve(feignException.status());
        if (status == null) {
            status = HttpStatus.BAD_GATEWAY;
        }
        String message = feignException.getMessage() != null
                ? feignException.getMessage()
                : "Upstream quote service error";
        return ResponseEntity.status(status).body(message);
    }
}
