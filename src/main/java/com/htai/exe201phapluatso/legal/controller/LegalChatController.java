package com.htai.exe201phapluatso.legal.controller;

import com.htai.exe201phapluatso.auth.repo.UserRepo;
import com.htai.exe201phapluatso.common.exception.NotFoundException;
import com.htai.exe201phapluatso.legal.dto.ChatRequest;
import com.htai.exe201phapluatso.legal.dto.ChatResponse;
import com.htai.exe201phapluatso.legal.service.LegalChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/legal/chat")
public class LegalChatController {

    private final LegalChatService chatService;
    private final UserRepo userRepo;

    public LegalChatController(LegalChatService chatService, UserRepo userRepo) {
        this.chatService = chatService;
        this.userRepo = userRepo;
    }

    /**
     * Chat with legal AI
     * POST /api/legal/chat/ask
     * Requires 1 chat credit per request
     */
    @PostMapping("/ask")
    public ResponseEntity<ChatResponse> ask(
            @RequestBody ChatRequest request,
            Authentication authentication
    ) {
        String email = authentication.getName();
        
        Long userId = userRepo.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"))
                .getId();
        
        ChatResponse response = chatService.chat(userId, request.question());
        return ResponseEntity.ok(response);
    }
}
