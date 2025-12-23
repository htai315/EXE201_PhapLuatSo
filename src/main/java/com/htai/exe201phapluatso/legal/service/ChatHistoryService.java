package com.htai.exe201phapluatso.legal.service;

import com.htai.exe201phapluatso.auth.entity.User;
import com.htai.exe201phapluatso.auth.repo.UserRepo;
import com.htai.exe201phapluatso.common.exception.ForbiddenException;
import com.htai.exe201phapluatso.common.exception.NotFoundException;
import com.htai.exe201phapluatso.legal.dto.*;
import com.htai.exe201phapluatso.legal.entity.ChatMessage;
import com.htai.exe201phapluatso.legal.entity.ChatSession;
import com.htai.exe201phapluatso.legal.entity.LegalArticle;
import com.htai.exe201phapluatso.legal.repo.ChatMessageRepo;
import com.htai.exe201phapluatso.legal.repo.ChatSessionRepo;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatHistoryService {

    private static final Logger log = LoggerFactory.getLogger(ChatHistoryService.class);
    private static final int MAX_TITLE_LENGTH = 50;

    private final ChatSessionRepo sessionRepo;
    private final ChatMessageRepo messageRepo;
    private final UserRepo userRepo;
    private final LegalChatService chatService;
    private final EntityManager entityManager;

    public ChatHistoryService(
            ChatSessionRepo sessionRepo,
            ChatMessageRepo messageRepo,
            UserRepo userRepo,
            LegalChatService chatService,
            EntityManager entityManager
    ) {
        this.sessionRepo = sessionRepo;
        this.messageRepo = messageRepo;
        this.userRepo = userRepo;
        this.chatService = chatService;
        this.entityManager = entityManager;
    }

    /**
     * Get all chat sessions for a user
     */
    @Transactional(readOnly = true)
    public List<ChatSessionDTO> getUserSessions(String userEmail) {
        User user = userRepo.findByEmail(userEmail)
                .orElseThrow(() -> new NotFoundException("User not found"));

        List<ChatSession> sessions = sessionRepo.findByUserIdOrderByUpdatedAtDesc(user.getId());

        return sessions.stream()
                .map(session -> new ChatSessionDTO(
                        session.getId(),
                        session.getTitle(),
                        session.getCreatedAt(),
                        session.getUpdatedAt(),
                        sessionRepo.countMessagesBySessionId(session.getId())
                ))
                .collect(Collectors.toList());
    }

    /**
     * Get messages in a session
     */
    @Transactional(readOnly = true)
    public List<ChatMessageDTO> getSessionMessages(String userEmail, Long sessionId) {
        ChatSession session = getSessionAndCheckOwnership(userEmail, sessionId);

        List<ChatMessage> messages = messageRepo.findBySessionIdWithCitations(sessionId);

        return messages.stream()
                .map(this::toMessageDTO)
                .collect(Collectors.toList());
    }

    /**
     * Send message in new or existing session
     */
    @Transactional
    public SendMessageResponse sendMessage(String userEmail, Long sessionId, String question) {
        User user = userRepo.findByEmail(userEmail)
                .orElseThrow(() -> new NotFoundException("User not found"));

        ChatSession session;
        if (sessionId == null) {
            // Create new session
            session = createNewSession(user, question);
        } else {
            // Use existing session
            session = getSessionAndCheckOwnership(userEmail, sessionId);
        }

        // Save user message
        ChatMessage userMessage = new ChatMessage();
        userMessage.setSession(session);
        userMessage.setRole("USER");
        userMessage.setContent(question);
        userMessage = messageRepo.save(userMessage);

        // Generate AI response
        ChatResponse chatResponse = chatService.chat(question);

        // Save assistant message with citations
        ChatMessage assistantMessage = new ChatMessage();
        assistantMessage.setSession(session);
        assistantMessage.setRole("ASSISTANT");
        assistantMessage.setContent(chatResponse.answer());
        
        // Add citations to message (use EntityManager.getReference to avoid loading full entities)
        if (chatResponse.citations() != null && !chatResponse.citations().isEmpty()) {
            List<LegalArticle> articles = chatResponse.citations().stream()
                    .map(citation -> entityManager.getReference(LegalArticle.class, citation.articleId()))
                    .collect(Collectors.toList());
            assistantMessage.setCitations(articles);
        }
        
        assistantMessage = messageRepo.save(assistantMessage);

        // Update session timestamp
        session.setUpdatedAt(LocalDateTime.now());
        sessionRepo.save(session);

        log.info("Message sent in session {} by user {}", session.getId(), userEmail);

        return new SendMessageResponse(
                session.getId(),
                toMessageDTO(userMessage),
                toMessageDTOFromResponse(assistantMessage, chatResponse.citations())
        );
    }

    /**
     * Delete a chat session
     */
    @Transactional
    public void deleteSession(String userEmail, Long sessionId) {
        ChatSession session = getSessionAndCheckOwnership(userEmail, sessionId);
        sessionRepo.delete(session);
        log.info("Session {} deleted by user {}", sessionId, userEmail);
    }

    // Helper methods

    private ChatSession createNewSession(User user, String firstQuestion) {
        ChatSession session = new ChatSession();
        session.setUser(user);
        session.setTitle(generateTitle(firstQuestion));
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());
        return sessionRepo.save(session);
    }

    private String generateTitle(String question) {
        if (question.length() <= MAX_TITLE_LENGTH) {
            return question;
        }
        return question.substring(0, MAX_TITLE_LENGTH) + "...";
    }

    private ChatSession getSessionAndCheckOwnership(String userEmail, Long sessionId) {
        ChatSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("Session not found"));

        if (!session.getUser().getEmail().equals(userEmail)) {
            throw new ForbiddenException("You don't have access to this session");
        }

        return session;
    }

    private ChatMessageDTO toMessageDTO(ChatMessage message) {
        List<CitationDTO> citations = message.getCitations().stream()
                .map(article -> new CitationDTO(
                        article.getId(),
                        article.getDocument().getDocumentName(),
                        article.getArticleNumber(),
                        article.getArticleTitle(),
                        truncate(article.getContent(), 200)
                ))
                .collect(Collectors.toList());

        return new ChatMessageDTO(
                message.getId(),
                message.getRole(),
                message.getContent(),
                citations,
                message.getCreatedAt()
        );
    }

    private ChatMessageDTO toMessageDTOFromResponse(ChatMessage message, List<CitationDTO> citations) {
        return new ChatMessageDTO(
                message.getId(),
                message.getRole(),
                message.getContent(),
                citations,
                message.getCreatedAt()
        );
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}
