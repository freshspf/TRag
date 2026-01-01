package com.spf.tbackend.memory;

public record ConversationMessage(Role role, String content) {
    public enum Role {
        USER,
        ASSISTANT
    }
}

