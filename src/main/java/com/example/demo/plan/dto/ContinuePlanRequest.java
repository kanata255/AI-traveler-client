package com.example.demo.plan.dto;

import jakarta.validation.constraints.NotBlank;

public record ContinuePlanRequest(@NotBlank(message = "message 不能为空") String message) {
}