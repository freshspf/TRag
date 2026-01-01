package com.spf.tbackend.controller.dto;

public record ChatResponse(
        String conversationId,
        String answer
) {
}

