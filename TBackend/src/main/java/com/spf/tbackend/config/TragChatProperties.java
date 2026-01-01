package com.spf.tbackend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "trag.chat")
public record TragChatProperties(
        String systemPrompt,
        int maxHistoryTurns,
        int maxMessageLength
) {
    public TragChatProperties {
        if (systemPrompt == null || systemPrompt.isBlank()) {
            systemPrompt = "你是一个严谨的中文助手。";
        }
        if (maxHistoryTurns <= 0) {
            maxHistoryTurns = 10;
        }
        if (maxMessageLength <= 0) {
            maxMessageLength = 2000;
        }
    }
}

