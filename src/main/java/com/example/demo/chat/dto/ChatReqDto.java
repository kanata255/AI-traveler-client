package com.example.demo.chat.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatReqDto(
        String sessionId,
        @NotBlank(message = "message 不能为空") String message
) {
}
