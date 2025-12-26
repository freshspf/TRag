package com.spf.tbackend.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "spring.ai.chat.client.enabled", havingValue = "false")
public class MultiChatClientConfigs {

    @Bean
    public ChatClient openAiChatClient(@Qualifier("openAiChatModel") ChatModel chatModel) {
        return ChatClient.create(chatModel);
    }

//    @Bean
//    public ChatClient deepSeekChatClient(DeepSeekChatModel chatModel) {
//        return ChatClient.builder(chatModel)
//                .defaultSystem("You are a friendly chat bot that answers question with json always")
//                .build();
//    }
}