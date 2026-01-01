package com.spf.tbackend.memory;

import java.util.List;

public interface ConversationMemoryStore {
    List<ConversationMessage> getRecentMessages(String conversationId, int maxHistoryTurns);

    void appendUserMessage(String conversationId, String content, int maxHistoryTurns);

    void appendAssistantMessage(String conversationId, String content, int maxHistoryTurns);
}

