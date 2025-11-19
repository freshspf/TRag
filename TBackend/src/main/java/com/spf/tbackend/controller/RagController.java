package com.spf.tbackend.controller;

import com.spf.tbackend.service.ChatService;
import com.spf.tbackend.service.IngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/rag")
@RequiredArgsConstructor
public class RagController {

    private final IngestionService ingestionService;
    private final ChatService chatService;

    @PostMapping("/upload")
    public String upload(@RequestParam("file") MultipartFile file) {
        ingestionService.ingestFile(file);
        return "Upload Success";
    }

    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@RequestParam("query") String query) {
        return chatService.streamChat(query);
    }
}
