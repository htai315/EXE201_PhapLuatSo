package com.htai.exe201phapluatso.legal.service;

import com.htai.exe201phapluatso.auth.entity.User;
import com.htai.exe201phapluatso.common.exception.ForbiddenException;
import com.htai.exe201phapluatso.common.exception.NotFoundException;
import com.htai.exe201phapluatso.credit.service.CreditService;
import com.htai.exe201phapluatso.credit.entity.CreditReservation;
import com.htai.exe201phapluatso.legal.dto.*;
import com.htai.exe201phapluatso.legal.entity.ChatMessage;
import com.htai.exe201phapluatso.legal.entity.ChatSession;
import com.htai.exe201phapluatso.legal.entity.LegalArticle;
import com.htai.exe201phapluatso.legal.repo.ChatMessageRepo;
import com.htai.exe201phapluatso.legal.repo.ChatSessionRepo;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ChatHistoryService {

    private static final Logger log = LoggerFactory.getLogger(ChatHistoryService.class);
    private static final int MAX_TITLE_LENGTH = 50;

    private final ChatSessionRepo sessionRepo;
    private final ChatMessageRepo messageRepo;
    private final LegalChatService chatService;
    private final CreditService creditService;
    private final EntityManager entityManager;
    private final MemoryService memoryService;

    public ChatHistoryService(
            ChatSessionRepo sessionRepo,
            ChatMessageRepo messageRepo,
            LegalChatService chatService,
            CreditService creditService,
            EntityManager entityManager,
            MemoryService memoryService) {
        this.sessionRepo = sessionRepo;
        this.messageRepo = messageRepo;
        this.chatService = chatService;
        this.creditService = creditService;
        this.entityManager = entityManager;
        this.memoryService = memoryService;
    }

    // ====== GET SESSIONS ======
    @Transactional(readOnly = true)
    public List<ChatSessionDTO> getUserSessions(Long userId, Integer page, Integer size, String search) {
        int pageNum = (page != null && page >= 0) ? page : 0;
        int pageSize = (size != null && size > 0 && size <= 100) ? size : 20;

        Pageable pageable = PageRequest.of(pageNum, pageSize);
        Page<ChatSession> sessionPage;

        if (search != null && !search.trim().isEmpty()) {
            sessionPage = sessionRepo.searchByUserIdAndTitle(userId, search.trim(), pageable);
        } else {
            sessionPage = sessionRepo.findByUserIdOrderByUpdatedAtDesc(userId, pageable);
        }

        List<ChatSession> sessions = sessionPage.getContent();
        if (sessions.isEmpty()) {
            return List.of();
        }

        List<Long> sessionIds = sessions.stream().map(ChatSession::getId).toList();
        Map<Long, Long> messageCounts = getMessageCountsMap(sessionIds);

        return sessions.stream()
                .map(session -> new ChatSessionDTO(
                        session.getId(),
                        session.getTitle(),
                        session.getCreatedAt(),
                        session.getUpdatedAt(),
                        messageCounts.getOrDefault(session.getId(), 0L)))
                .toList();
    }

    private Map<Long, Long> getMessageCountsMap(List<Long> sessionIds) {
        if (sessionIds.isEmpty()) {
            return Map.of();
        }
        List<Object[]> results = sessionRepo.countMessagesBySessionIds(sessionIds);
        Map<Long, Long> map = new java.util.HashMap<>();
        for (Object[] row : results) {
            Long sessionId = ((Number) row[0]).longValue();
            Long count = ((Number) row[1]).longValue();
            map.put(sessionId, count);
        }
        return map;
    }

    @Transactional(readOnly = true)
    public long getUserSessionsCount(Long userId, String search) {
        if (search != null && !search.trim().isEmpty()) {
            Pageable pageable = PageRequest.of(0, 1);
            return sessionRepo.searchByUserIdAndTitle(userId, search.trim(), pageable).getTotalElements();
        }
        return sessionRepo.findByUserIdOrderByUpdatedAtDesc(userId).size();
    }

    // ====== GET MESSAGES ======
    @Transactional(readOnly = true)
    public List<ChatMessageDTO> getSessionMessages(Long userId, Long sessionId) {
        getSessionAndCheckOwnership(userId, sessionId); // Validates ownership
        List<ChatMessage> messages = messageRepo.findBySessionIdWithCitations(sessionId);
        return messages.stream().map(this::toMessageDTO).toList();
    }

    // ====== SEND MESSAGE ORCHESTRATOR ======
    /**
     * Send message in new or existing session.
     * 
     * TRUE 3-PHASE DESIGN:
     * - Phase A: Short @Transactional (validation + charge + increment count ONLY)
     * - Phase B: NO transaction (AI call)
     * - Phase C: Short @Transactional (confirm/refund + persist messages)
     */
    public SendMessageResponse sendMessage(Long userId, String userEmail, Long sessionId, String question) {
        PhaseAResult phaseAResult = executePhaseAWithRetry(userId, userEmail, sessionId, question);

        ChatResponse chatResponse;
        try {
            chatResponse = chatService.chat(
                    phaseAResult.userId(),
                    question,
                    phaseAResult.conversationContext());
        } catch (Exception e) {
            executePhaseCFailure(phaseAResult);
            throw new com.htai.exe201phapluatso.common.exception.AiChatFailedException(
                    "AI không thể xử lý yêu cầu. Vui lòng thử lại sau.", e);
        }

        return executePhaseCSuccess(phaseAResult, question, chatResponse);
    }

    private PhaseAResult executePhaseAWithRetry(Long userId, String userEmail, Long sessionId, String question) {
        int maxRetries = 2;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                return executePhaseA(userId, userEmail, sessionId, question);
            } catch (org.springframework.dao.OptimisticLockingFailureException e) {
                if (attempt >= maxRetries) {
                    log.warn("Phase A failed after {} retries for session {}", maxRetries + 1, sessionId);
                    throw e;
                }
                log.info("Optimistic lock conflict on attempt {}, retrying...", attempt + 1);
                try {
                    Thread.sleep(50L * (attempt + 1));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new com.htai.exe201phapluatso.common.exception.BadRequestException("Yêu cầu bị gián đoạn.");
                }
            }
        }
        throw new com.htai.exe201phapluatso.common.exception.BadRequestException(
                "Không thể xử lý yêu cầu. Vui lòng thử lại.");
    }

    // ====== PHASE A ======
    @Transactional
    public PhaseAResult executePhaseA(Long userId, String userEmail, Long sessionId, String question) {
        ChatSession session;
        ConversationContext conversationContext = null;
        Long reservationId = null;
        boolean wasFirstChargeAttempt = false;

        if (sessionId == null) {
            // Create new session using userId reference (no user query)
            session = createNewSession(userId, question);

            CreditReservation reservation = creditService.reserveSessionCredit(userId, session.getId());
            session.setChargeState("CHARGING");
            session.setChargeReservationId(reservation.getId());
            session.setUserQuestionCount(1);
            sessionRepo.save(session);

            reservationId = reservation.getId();
            wasFirstChargeAttempt = true;
        } else {
            // Use existing session - ownership checked by findByIdAndUserId
            session = getSessionAndCheckOwnership(userId, sessionId);

            if (session.getUserQuestionCount() >= 10) {
                throw new com.htai.exe201phapluatso.common.exception.SessionLimitExceededException(
                        "Phiên chat đã đạt 10 câu hỏi. Vui lòng tạo phiên mới.");
            }

            if ("NOT_CHARGED".equals(session.getChargeState())) {
                CreditReservation reservation = creditService.reserveSessionCredit(userId, session.getId());
                session.setChargeState("CHARGING");
                session.setChargeReservationId(reservation.getId());
                reservationId = reservation.getId();
                wasFirstChargeAttempt = true;
            }

            session.setUserQuestionCount(session.getUserQuestionCount() + 1);
            sessionRepo.save(session);

            conversationContext = memoryService.buildConversationContext(sessionId);
        }

        return new PhaseAResult(
                userId,
                session.getId(),
                reservationId,
                wasFirstChargeAttempt,
                session.getUserQuestionCount(),
                conversationContext,
                userEmail,
                question);
    }

    // ====== PHASE C FAILURE ======
    @Transactional
    public void executePhaseCFailure(PhaseAResult phaseAResult) {
        if (phaseAResult.wasFirstChargeAttempt()
                && phaseAResult.reservationId() != null
                && phaseAResult.questionNumber() == 1) {

            ChatSession session = sessionRepo.findById(phaseAResult.sessionId()).orElse(null);

            if (session != null && "CHARGING".equals(session.getChargeState())) {
                creditService.refundReservation(phaseAResult.reservationId());
                session.setChargeState("NOT_CHARGED");
                session.setChargeReservationId(null);
                session.setUserQuestionCount(0);
                sessionRepo.save(session);
                log.info("Refunded first question failure for session {}", phaseAResult.sessionId());
            }
        }
    }

    // ====== PHASE C SUCCESS ======
    @Transactional
    public SendMessageResponse executePhaseCSuccess(PhaseAResult phaseAResult, String question,
            ChatResponse chatResponse) {
        ChatSession session = sessionRepo.findById(phaseAResult.sessionId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên chat"));

        if (phaseAResult.wasFirstChargeAttempt()
                && phaseAResult.reservationId() != null
                && "CHARGING".equals(session.getChargeState())) {
            creditService.confirmReservation(phaseAResult.reservationId());
            session.setChargeState("CHARGED");
            log.info("Confirmed session charge for session {}", session.getId());
        }

        ChatMessage userMessage = new ChatMessage();
        userMessage.setSession(session);
        userMessage.setRole("USER");
        userMessage.setContent(phaseAResult.userQuestion());
        userMessage = messageRepo.save(userMessage);

        ChatMessage assistantMessage = new ChatMessage();
        assistantMessage.setSession(session);
        assistantMessage.setRole("ASSISTANT");
        assistantMessage.setContent(chatResponse.answer());

        if (chatResponse.citations() != null && !chatResponse.citations().isEmpty()) {
            List<LegalArticle> articles = chatResponse.citations().stream()
                    .map(citation -> entityManager.getReference(LegalArticle.class, citation.articleId()))
                    .collect(Collectors.toList());
            assistantMessage.setCitations(articles);
        }
        assistantMessage = messageRepo.save(assistantMessage);

        session.setUpdatedAt(LocalDateTime.now());
        sessionRepo.save(session);

        log.info("Message sent in session {} by user {} (with context: {})",
                session.getId(), phaseAResult.userEmail(),
                phaseAResult.conversationContext() != null && !phaseAResult.conversationContext().isEmpty());

        return new SendMessageResponse(
                session.getId(),
                toMessageDTO(userMessage),
                toMessageDTOFromResponse(assistantMessage, chatResponse.citations()));
    }

    public record PhaseAResult(
            Long userId,
            Long sessionId,
            Long reservationId,
            boolean wasFirstChargeAttempt,
            int questionNumber,
            ConversationContext conversationContext,
            String userEmail,
            String userQuestion) {
    }

    // ====== DELETE ======
    @Transactional
    public void deleteSession(Long userId, Long sessionId) {
        ChatSession session = getSessionAndCheckOwnership(userId, sessionId);
        sessionRepo.delete(session);
        log.info("Session {} deleted by user {}", sessionId, userId);
    }

    // ====== HELPER METHODS ======

    private ChatSession createNewSession(Long userId, String firstQuestion) {
        ChatSession session = new ChatSession();
        // Use reference to avoid loading User entity
        User userRef = entityManager.getReference(User.class, userId);
        session.setUser(userRef);
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

    /**
     * Ownership check using findByIdAndUserId - NO lazy user access
     */
    private ChatSession getSessionAndCheckOwnership(Long userId, Long sessionId) {
        return sessionRepo.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> {
                    if (!sessionRepo.existsById(sessionId)) {
                        return new NotFoundException("Không tìm thấy phiên chat");
                    }
                    return new ForbiddenException("Bạn không có quyền truy cập phiên chat này");
                });
    }

    private ChatMessageDTO toMessageDTO(ChatMessage message) {
        List<CitationDTO> citations = message.getCitations().stream()
                .map(article -> new CitationDTO(
                        article.getId(),
                        article.getDocument().getDocumentName(),
                        article.getArticleNumber(),
                        article.getArticleTitle(),
                        truncate(article.getContent(), 200)))
                .collect(Collectors.toList());

        return new ChatMessageDTO(
                message.getId(),
                message.getRole(),
                message.getContent(),
                citations,
                message.getCreatedAt());
    }

    private ChatMessageDTO toMessageDTOFromResponse(ChatMessage message, List<CitationDTO> citations) {
        return new ChatMessageDTO(
                message.getId(),
                message.getRole(),
                message.getContent(),
                citations,
                message.getCreatedAt());
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}
