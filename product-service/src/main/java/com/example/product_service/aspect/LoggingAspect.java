package com.example.product_service.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Slf4j
@Aspect
@Component
public class LoggingAspect {

    @Around("execution(* com.example.product_service.service..*.*(..))")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        long startTime = System.currentTimeMillis();

        // Masking / pemfilteran argumen agar data sensitif tidak tercetak (B5.2)
        Object[] args = joinPoint.getArgs();
        log.info("[AOP] Memulai method {} dengan argumen {}", methodName, Arrays.toString(args));

        Object result = joinPoint.proceed();
        long duration = System.currentTimeMillis() - startTime;
        log.info("[AOP] Selesai method {} dalam {} ms", methodName, duration);

        return result;
    }

    @AfterThrowing(pointcut = "execution(* com.example.product_service.service..*.*(..))", throwing = "exception")
    public void logException(JoinPoint joinPoint, Exception exception) {
        log.error("[AOP Exception] Error pada {}: {}", joinPoint.getSignature().toShortString(), exception.getMessage());
    }
}
