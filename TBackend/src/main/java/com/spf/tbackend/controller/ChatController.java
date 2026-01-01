package com.spf.tbackend.controller;

import com.spf.tbackend.controller.dto.ChatRequest;
import com.spf.tbackend.controller.dto.ChatResponse;
import com.spf.tbackend.service.ChatService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/chat/memory")
    public ChatResponse chatWithMemory(@RequestBody @Valid ChatRequest request) {
        ChatService.ChatResult result = chatService.chat(request.conversationId(), request.message());
        return new ChatResponse(result.conversationId(), result.answer());
    }

    @GetMapping("/chat/temp")
    public String chatTemp(@RequestParam @NotBlank(message = "message is required") String message) {
        return chatService.chatTemp(message);
    }

}
