package com.spf.tbackend.memory;

import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryConversationMemoryStore implements ConversationMemoryStore {

    private final Map<String, Deque<ConversationMessage>> store = new ConcurrentHashMap<>();

    @Override
    public List<ConversationMessage> getRecentMessages(String conversationId, int maxHistoryTurns) {
        Deque<ConversationMessage> deque = store.get(conversationId);
        if (deque == null) {
            return List.of();
        }

        int maxMessages = Math.max(0, maxHistoryTurns) * 2;
        synchronized (deque) {
            if (maxMessages == 0 || deque.isEmpty()) {
                return List.of();
            }
            int skip = Math.max(0, deque.size() - maxMessages);
            List<ConversationMessage> result = new ArrayList<>(Math.min(deque.size(), maxMessages));
            int index = 0;
            for (ConversationMessage message : deque) {
                if (index++ >= skip) {
                    result.add(message);
                }
            }
            return result;
        }
    }

    @Override
    public void appendUserMessage(String conversationId, String content, int maxHistoryTurns) {
        append(conversationId, new ConversationMessage(ConversationMessage.Role.USER, content), maxHistoryTurns);
    }

    @Override
    public void appendAssistantMessage(String conversationId, String content, int maxHistoryTurns) {
        append(conversationId, new ConversationMessage(ConversationMessage.Role.ASSISTANT, content), maxHistoryTurns);
    }

    private void append(String conversationId, ConversationMessage message, int maxHistoryTurns) {
        Deque<ConversationMessage> deque = store.computeIfAbsent(conversationId, key -> new ArrayDeque<>());
        int maxMessages = Math.max(0, maxHistoryTurns) * 2;
        synchronized (deque) {
            deque.addLast(message);
            if (maxMessages > 0) {
                while (deque.size() > maxMessages) {
                    deque.removeFirst();
                }
            }
        }
    }
}

