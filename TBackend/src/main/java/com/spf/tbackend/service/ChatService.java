package com.spf.tbackend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatClient.Builder chatClientBuilder;
    private final VectorStore vectorStore;

    public Flux<String> streamChat(String userQuery) {

        ChatClient chatClient = chatClientBuilder.build();

        return chatClient.prompt()
                .user(userQuery)
                .advisors(
                        new QuestionAnswerAdvisor(
                                vectorStore,
                                SearchRequest.defaults()
                                        .withTopK(3)
                                        .withSimilarityThreshold(0.7)
                        )
                )
                .stream()
                .content();
    }
}