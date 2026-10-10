package com.example.demo.aspect;

import cn.hutool.json.JSONUtil;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.stream.Collectors;

@Slf4j
@Aspect
@Component
public class ApiLogAspect {

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    @Around("@within(org.springframework.web.bind.annotation.RestController)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String api = signature.getDeclaringType().getSimpleName() + "#" + signature.getName();
        String params = Arrays.stream(joinPoint.getArgs())
                .filter(ApiLogAspect::isLoggable)
                .map(ApiLogAspect::toJson)
                .collect(Collectors.joining(", "));
        log.info("[接口调用] 接口={} 参数=[{}] 时间={}", api, params, LocalDateTime.now().format(TIME_FORMATTER));
        return joinPoint.proceed();
    }

    private static boolean isLoggable(Object arg) {
        if (arg == null) {
            return false;
        }
        return !(arg instanceof ServletRequest)
                && !(arg instanceof ServletResponse)
                && !(arg instanceof MultipartFile);
    }

    private static String toJson(Object arg) {
        try {
            return maskSensitive(JSONUtil.toJsonStr(arg));
        } catch (Exception e) {
            return String.valueOf(arg);
        }
    }

    private static String maskSensitive(String json) {
        return json.replaceAll("(?i)(\"(?:password|pwd|token|secret|authorization)\"\\s*:\\s*\")[^\"]*(\")", "$1***$2");
    }
}