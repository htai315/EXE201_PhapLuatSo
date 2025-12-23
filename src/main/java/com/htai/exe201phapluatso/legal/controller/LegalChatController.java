package com.htai.exe201phapluatso.legal.controller;

import com.htai.exe201phapluatso.legal.dto.ChatRequest;
import com.htai.exe201phapluatso.legal.dto.ChatResponse;
import com.htai.exe201phapluatso.legal.service.LegalChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/legal/chat")
public class LegalChatController {

    private final LegalChatService chatService;

    public LegalChatController(LegalChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * Chat with legal AI
     * POST /api/legal/chat/ask
     */
    @PostMapping("/ask")
    public ResponseEntity<ChatResponse> ask(@RequestBody ChatRequest request) {
        ChatResponse response = chatService.chat(request.question());
        return ResponseEntity.ok(response);
    }
}
