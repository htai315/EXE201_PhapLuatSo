package com.htai.exe201phapluatso.legal.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds conversation history for context-aware chat responses
 * Limits to recent messages to avoid token overflow
 */
public class ConversationContext {
    
    private static final int MAX_MESSAGES = 6; // 3 pairs of user/assistant
    
    private final List<Message> messages;
    
    public ConversationContext() {
        this.messages = new ArrayList<>();
    }
    
    public ConversationContext(List<Message> messages) {
        // Only keep the most recent messages
        if (messages.size() > MAX_MESSAGES) {
            this.messages = new ArrayList<>(messages.subList(messages.size() - MAX_MESSAGES, messages.size()));
        } else {
            this.messages = new ArrayList<>(messages);
        }
    }
    
    public void addMessage(String role, String content) {
        messages.add(new Message(role, content));
        // Trim if exceeds max
        while (messages.size() > MAX_MESSAGES) {
            messages.remove(0);
        }
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
    public record Message(String role, String content) {}
}
