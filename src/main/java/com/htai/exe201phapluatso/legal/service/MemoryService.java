package com.htai.exe201phapluatso.legal.service;

import com.htai.exe201phapluatso.legal.dto.ConversationContext;
import com.htai.exe201phapluatso.legal.entity.ChatMessage;
import com.htai.exe201phapluatso.legal.repo.ChatMessageRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Responsible for building and managing conversation memory/context.
 * Extracted from ChatHistoryService for better separation of concerns.
 * 
 * Single responsibility:
 * - Build ConversationContext from chat messages
 * - Handle token/message limits
 * - Prepare context for AI processing
 */
@Service
public class MemoryService {

    private static final Logger log = LoggerFactory.getLogger(MemoryService.class);

    private final ChatMessageRepo messageRepo;

    public MemoryService(ChatMessageRepo messageRepo) {
        this.messageRepo = messageRepo;
    }

    /**
     * Build conversation context from previous messages in a session.
     * Context is used to help AI understand conversation history.
     * 
     * @param sessionId Chat session ID
     * @return ConversationContext with recent messages, or null if no messages
     *         exist
     */
    @Transactional(readOnly = true)
    public ConversationContext buildConversationContext(Long sessionId) {
        if (sessionId == null) {
            return null;
        }

        List<ChatMessage> messages = messageRepo.findBySessionIdWithCitations(sessionId);

        if (messages.isEmpty()) {
            return null;
        }

        // Convert to ConversationContext.Message format
        List<ConversationContext.Message> contextMessages = messages.stream()
                .map(msg -> new ConversationContext.Message(msg.getRole(), msg.getContent()))
                .collect(Collectors.toList());

        log.debug("Built conversation context with {} messages for session {}",
                contextMessages.size(), sessionId);

        return new ConversationContext(contextMessages);
    }

    /**
     * Build context from a list of ChatMessage entities.
     * Useful when messages are already loaded.
     * 
     * @param messages List of chat messages
     * @return ConversationContext or null if empty
     */
    public ConversationContext buildContextFromMessages(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return null;
        }

        List<ConversationContext.Message> contextMessages = messages.stream()
                .map(msg -> new ConversationContext.Message(msg.getRole(), msg.getContent()))
                .collect(Collectors.toList());

        return new ConversationContext(contextMessages);
    }

    /**
     * Check if context exceeds recommended token limits.
     * Simple estimation: ~4 chars per token.
     * 
     * @param context Conversation context to check
     * @return true if context may cause token overflow
     */
    public boolean isContextTooLarge(ConversationContext context) {
        if (context == null || context.isEmpty()) {
            return false;
        }

        int estimatedTokens = context.getMessages().stream()
                .mapToInt(msg -> estimateTokens(msg.content()))
                .sum();

        // Warn if context alone exceeds 2000 tokens (leaving room for prompt +
        // response)
        boolean tooLarge = estimatedTokens > 2000;

        if (tooLarge) {
            log.warn("Conversation context may be too large: ~{} tokens", estimatedTokens);
        }

        return tooLarge;
    }

    /**
     * Estimate token count for a text string.
     * Simple heuristic: ~4 characters per token for Vietnamese text.
     */
    private int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return text.length() / 4;
    }
}
