package com.htai.exe201phapluatso.legal.controller;

import com.htai.exe201phapluatso.auth.security.AuthUserPrincipal;
import com.htai.exe201phapluatso.common.exception.UnauthorizedException;
import com.htai.exe201phapluatso.legal.dto.*;
import com.htai.exe201phapluatso.legal.service.ChatHistoryService;
import jakarta.validation.Valid;
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

    @GetMapping("/sessions")
    public ResponseEntity<Map<String, Object>> getSessions(
            Authentication auth,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String search) {
        Long userId = getUserId(auth);

        List<ChatSessionDTO> sessions = chatHistoryService.getUserSessions(userId, page, size, search);
        long totalCount = chatHistoryService.getUserSessionsCount(userId, search);

        int pageNum = (page != null && page >= 0) ? page : 0;
        int pageSize = (size != null && size > 0 && size <= 100) ? size : 20;
        int totalPages = (int) Math.ceil((double) totalCount / pageSize);
        boolean hasMore = (pageNum + 1) < totalPages;

        Map<String, Object> response = new HashMap<>();
        response.put("sessions", sessions);
        response.put("page", pageNum);
        response.put("size", pageSize);
        response.put("totalCount", totalCount);
        response.put("totalPages", totalPages);
        response.put("hasMore", hasMore);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<List<ChatMessageDTO>> getMessages(
            Authentication auth,
            @PathVariable Long sessionId) {
        Long userId = getUserId(auth);
        List<ChatMessageDTO> messages = chatHistoryService.getSessionMessages(userId, sessionId);
        return ResponseEntity.ok(messages);
    }

    @PostMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<SendMessageResponse> sendMessageInSession(
            Authentication auth,
            @PathVariable Long sessionId,
            @Valid @RequestBody SendMessageRequest request) {
        Long userId = getUserId(auth);
        String email = getUserEmail(auth);

        SendMessageResponse response = chatHistoryService.sendMessage(
                userId,
                email,
                sessionId,
                request.question());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/sessions/messages")
    public ResponseEntity<SendMessageResponse> sendMessageNewSession(
            Authentication auth,
            @Valid @RequestBody SendMessageRequest request) {
        Long userId = getUserId(auth);
        String email = getUserEmail(auth);

        SendMessageResponse response = chatHistoryService.sendMessage(
                userId,
                email,
                null,
                request.question());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> deleteSession(
            Authentication auth,
            @PathVariable Long sessionId) {
        Long userId = getUserId(auth);
        chatHistoryService.deleteSession(userId, sessionId);
        return ResponseEntity.noContent().build();
    }

    private AuthUserPrincipal requirePrincipal(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) {
            throw new UnauthorizedException("Unauthorized");
        }
        if (!(auth.getPrincipal() instanceof AuthUserPrincipal p)) {
            throw new UnauthorizedException("Unauthorized");
        }
        return p;
    }

    private Long getUserId(Authentication auth) {
        return requirePrincipal(auth).userId();
    }

    private String getUserEmail(Authentication auth) {
        return requirePrincipal(auth).email();
    }
}
