package com.spf.tbackend.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(
        String conversationId,
        @NotBlank(message = "message is required")
        String message
) {
}
