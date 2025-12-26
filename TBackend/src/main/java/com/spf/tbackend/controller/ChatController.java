package com.spf.tbackend.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/multi-chat")
public class ChatController {

    @Autowired
    @Qualifier("openAiChatClient")
    private ChatClient openAiChatClient;


    @GetMapping("/chat")
    public String chat(String message) {
        return openAiChatClient.prompt("you are gpt-4o-mini").user("Who are you")
                .call().content();
    }


}
