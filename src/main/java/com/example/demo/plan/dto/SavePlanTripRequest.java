package com.example.demo.plan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record SavePlanTripRequest(
        @NotBlank(message = "planTaskId 不能为空") String planTaskId,
        @NotNull(message = "trip 不能为空") Map<String, Object> trip
) {
}