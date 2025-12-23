package com.htai.exe201phapluatso.legal.controller;

import com.htai.exe201phapluatso.auth.security.AuthUserPrincipal;
import com.htai.exe201phapluatso.common.exception.UnauthorizedException;
import com.htai.exe201phapluatso.legal.dto.*;
import com.htai.exe201phapluatso.legal.service.ChatHistoryService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatHistoryController {

    private final ChatHistoryService chatHistoryService;

    public ChatHistoryController(ChatHistoryService chatHistoryService) {
        this.chatHistoryService = chatHistoryService;
    }

    /**
     * Get all chat sessions for current user (with pagination and search)
     */
    @GetMapping("/sessions")
    public ResponseEntity<Map<String, Object>> getSessions(
            Authentication auth,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String search
    ) {
        String userEmail = getUserEmail(auth);
        
        // Get sessions
        List<ChatSessionDTO> sessions = chatHistoryService.getUserSessions(userEmail, page, size, search);
        
        // Get total count
        long totalCount = chatHistoryService.getUserSessionsCount(userEmail, search);
        
        // Calculate pagination info
        int pageNum = (page != null && page >= 0) ? page : 0;
        int pageSize = (size != null && size > 0 && size <= 100) ? size : 20;
        int totalPages = (int) Math.ceil((double) totalCount / pageSize);
        boolean hasMore = (pageNum + 1) < totalPages;
        
        // Build response
        Map<String, Object> response = new HashMap<>();
        response.put("sessions", sessions);
        response.put("page", pageNum);
        response.put("size", pageSize);
        response.put("totalCount", totalCount);
        response.put("totalPages", totalPages);
        response.put("hasMore", hasMore);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get messages in a session
     */
    @GetMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<List<ChatMessageDTO>> getMessages(
            Authentication auth,
            @PathVariable Long sessionId
    ) {
        String userEmail = getUserEmail(auth);
        List<ChatMessageDTO> messages = chatHistoryService.getSessionMessages(userEmail, sessionId);
        return ResponseEntity.ok(messages);
    }

    /**
     * Send message in existing session
     */
    @PostMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<SendMessageResponse> sendMessageInSession(
            Authentication auth,
            @PathVariable Long sessionId,
            @Valid @RequestBody SendMessageRequest request
    ) {
        String userEmail = getUserEmail(auth);
        SendMessageResponse response = chatHistoryService.sendMessage(
                userEmail,
                sessionId,
                request.question()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Send message in new session
     */
    @PostMapping("/sessions/messages")
    public ResponseEntity<SendMessageResponse> sendMessageNewSession(
            Authentication auth,
            @Valid @RequestBody SendMessageRequest request
    ) {
        String userEmail = getUserEmail(auth);
        SendMessageResponse response = chatHistoryService.sendMessage(
                userEmail,
                null,
                request.question()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a session
     */
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> deleteSession(
            Authentication auth,
            @PathVariable Long sessionId
    ) {
        String userEmail = getUserEmail(auth);
        chatHistoryService.deleteSession(userEmail, sessionId);
        return ResponseEntity.noContent().build();
    }

    private String getUserEmail(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) {
            throw new UnauthorizedException("Unauthorized");
        }
        AuthUserPrincipal principal = (AuthUserPrincipal) auth.getPrincipal();
        return principal.email();
    }
}
