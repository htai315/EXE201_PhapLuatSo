package com.htai.exe201phapluatso.legal.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds conversation history for context-aware chat responses.
 * Uses token-based limits (instead of message count) for more adaptive context
 * management.
 * 
 * Token estimation: ~4 characters per token for Vietnamese text.
 */
public class ConversationContext {

    /**
     * Maximum tokens for conversation context.
     * Leaves room for prompt (~2000 tokens) + response (~1000 tokens) within model
     * limit.
     */
    private static final int MAX_TOKENS = 2000;

    /**
     * Characters per token estimate for Vietnamese text.
     * Vietnamese uses more characters per token than English.
     */
    private static final int CHARS_PER_TOKEN = 4;

    /**
     * Fallback: Maximum messages to prevent extremely long contexts.
     * Even with token limits, cap at 10 messages for performance.
     */
    private static final int MAX_MESSAGES_HARD_LIMIT = 10;

    private final List<Message> messages;

    public ConversationContext() {
        this.messages = new ArrayList<>();
    }

    public ConversationContext(List<Message> messages) {
        this.messages = new ArrayList<>();
        // Add messages while respecting token limit
        for (Message msg : messages) {
            addMessage(msg.role(), msg.content());
        }
    }

    /**
     * Add message while respecting token limits.
     * Removes oldest messages if adding would exceed token budget.
     */
    public void addMessage(String role, String content) {
        messages.add(new Message(role, content));

        // Remove oldest messages until within token budget
        trimToTokenLimit();

        // Also enforce hard message limit
        while (messages.size() > MAX_MESSAGES_HARD_LIMIT) {
            messages.remove(0);
        }
    }

    /**
     * Trim messages from the beginning until total tokens is within limit.
     */
    private void trimToTokenLimit() {
        while (calculateTotalTokens() > MAX_TOKENS && messages.size() > 1) {
            messages.remove(0);
        }
    }

    /**
     * Calculate total estimated tokens in current context.
     */
    public int calculateTotalTokens() {
        return messages.stream()
                .mapToInt(msg -> estimateTokens(msg.content()))
                .sum();
    }

    /**
     * Estimate token count for a text string.
     * Simple heuristic: ~4 characters per token for Vietnamese text.
     */
    private int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return text.length() / CHARS_PER_TOKEN;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public boolean isEmpty() {
        return messages.isEmpty();
    }

    /**
     * Get the last assistant message content (for search enhancement)
     */
    public String getLastAssistantMessage() {
        for (int i = messages.size() - 1; i >= 0; i--) {
            if ("ASSISTANT".equals(messages.get(i).role())) {
                return messages.get(i).content();
            }
        }
        return null;
    }

    /**
     * Get the last user message content
     */
    public String getLastUserMessage() {
        for (int i = messages.size() - 1; i >= 0; i--) {
            if ("USER".equals(messages.get(i).role())) {
                return messages.get(i).content();
            }
        }
        return null;
    }

    /**
     * Create context from chat message entities
     */
    public static ConversationContext fromMessages(List<? extends MessageLike> chatMessages) {
        List<Message> messages = new ArrayList<>();
        for (MessageLike msg : chatMessages) {
            messages.add(new Message(msg.getRole(), msg.getContent()));
        }
        return new ConversationContext(messages);
    }

    /**
     * Interface for message-like objects
     */
    public interface MessageLike {
        String getRole();

        String getContent();
    }

    /**
     * Simple message record
     */
    public record Message(String role, String content) {
    }
}
