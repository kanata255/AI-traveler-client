package com.example.demo.plan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record CreatePlanTaskRequest(
        @NotBlank(message = "destination 不能为空") String destination,
        String origin,
        @NotBlank(message = "startDate 不能为空") String startDate,
        @NotBlank(message = "endDate 不能为空") String endDate,
        @NotNull(message = "travelers 不能为空") Integer travelers,
        BigDecimal budget,
        @NotBlank(message = "currency 不能为空") String currency,
        String pace,
        List<String> interests,
        String accommodation,
        String transport,
        List<String> dietary,
        String mustVisit,
        String avoid,
        String extraNote,
        String previousPlan,
        String modifyRequest
) {
}
