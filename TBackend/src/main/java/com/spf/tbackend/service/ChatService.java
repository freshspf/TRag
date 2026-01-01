package com.spf.tbackend.service;

import com.spf.tbackend.config.TragChatProperties;
import com.spf.tbackend.controller.ChatModelException;
import com.spf.tbackend.memory.ConversationMemoryStore;
import com.spf.tbackend.memory.ConversationMessage;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class ChatService {

    private final ChatClient chatClient;
    private final ConversationMemoryStore memoryStore;
    private final TragChatProperties properties;

    public ChatService(
            @Qualifier("openAiChatClient") ChatClient chatClient,
            ConversationMemoryStore memoryStore,
            TragChatProperties properties
    ) {
        this.chatClient = chatClient;
        this.memoryStore = memoryStore;
        this.properties = properties;
    }

    public ChatResult chat(String conversationId, String message) {
        return chatInternal(conversationId, message, true);
    }

    public String chatTemp(String message) {
        return chatInternal(null, message, false).answer();
    }

    private ChatResult chatInternal(String conversationId, String message, boolean useMemory) {
        String trimmed = message == null ? "" : message.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("message is required");
        }
        if (trimmed.length() > properties.maxMessageLength()) {
            throw new IllegalArgumentException("message too long");
        }

        String finalConversationId = (conversationId == null || conversationId.isBlank())
                ? UUID.randomUUID().toString()
                : conversationId;

        List<org.springframework.ai.chat.messages.Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(properties.systemPrompt()));

        if (useMemory) {
            List<ConversationMessage> history = memoryStore.getRecentMessages(finalConversationId, properties.maxHistoryTurns());
            for (ConversationMessage historyMessage : history) {
                if (historyMessage.role() == ConversationMessage.Role.USER) {
                    messages.add(new UserMessage(historyMessage.content()));
                } else {
                    messages.add(new AssistantMessage(historyMessage.content()));
                }
            }
        }

        messages.add(new UserMessage(trimmed));
        if (useMemory) {
            memoryStore.appendUserMessage(finalConversationId, trimmed, properties.maxHistoryTurns());
        }

        String answer;
        try {
            answer = chatClient.prompt()
                    .messages(messages)
                    .call()
                    .content();
        } catch (Exception ex) {
            throw new ChatModelException("model call failed", ex);
        }

        String finalAnswer = Objects.toString(answer, "");
        if (useMemory) {
            memoryStore.appendAssistantMessage(finalConversationId, finalAnswer, properties.maxHistoryTurns());
        }
        return new ChatResult(finalConversationId, finalAnswer);
    }

    public record ChatResult(String conversationId, String answer) {
    }
}
