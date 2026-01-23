package com.htai.exe201phapluatso.legal.service;

import com.htai.exe201phapluatso.ai.service.OpenAIService;
import com.htai.exe201phapluatso.common.exception.BadRequestException;
import com.htai.exe201phapluatso.credit.service.CreditService;
import com.htai.exe201phapluatso.legal.config.LegalSearchConfig;
import com.htai.exe201phapluatso.legal.dto.ChatResponse;
import com.htai.exe201phapluatso.legal.dto.CitationDTO;
import com.htai.exe201phapluatso.legal.dto.ConversationContext;
import com.htai.exe201phapluatso.legal.entity.LegalArticle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for RAG-based legal chatbot
 * Implements Retrieval-Augmented Generation pattern with credits system
 * Supports both regular and streaming responses
 * 
 * Responsibilities (after refactoring):
 * - Orchestrate chat flow: validate → reserve credit → search → generate →
 * confirm
 * - Delegate prompt building to PromptBuilder
 * - Delegate search to LegalSearchService
 */
@Service
public class LegalChatService {

    private static final Logger log = LoggerFactory.getLogger(LegalChatService.class);

    private final LegalSearchService searchService;
    private final OpenAIService aiService;
    private final CreditService creditService;
    private final PromptBuilder promptBuilder;

    public LegalChatService(
            LegalSearchService searchService,
            OpenAIService aiService,
            CreditService creditService,
            PromptBuilder promptBuilder) {
        this.searchService = searchService;
        this.aiService = aiService;
        this.creditService = creditService;
        this.promptBuilder = promptBuilder;
    }

    /**
     * Process user question using RAG pipeline with AI-powered re-ranking
     * Requires 1 chat credit per request
     * 
     * @param userId   User ID (for credit checking)
     * @param question User's legal question
     * @return AI-generated answer with citations
     * @throws com.htai.exe201phapluatso.common.exception.ForbiddenException if
     *                                                                       insufficient
     *                                                                       credits
     */
    public ChatResponse chat(Long userId, String question) {
        return chat(userId, question, null);
    }

    /**
     * Process user question with conversation context (memory)
     * This allows AI to understand context from previous messages in the session
     * 
     * @param userId              User ID (for credit checking)
     * @param question            User's legal question
     * @param conversationContext Previous messages in the session (can be null)
     * @return AI-generated answer with citations
     */
    public ChatResponse chat(Long userId, String question, ConversationContext conversationContext) {
        validateQuestion(question);

        log.info("Processing chat question for user {}: {}", userId, question);

        try {
            // Step 1: Unified search with context enhancement and AI re-ranking
            var searchResult = searchService.searchForChat(question, LegalSearchConfig.DEFAULT_SEARCH_LIMIT,
                    conversationContext);
            List<LegalArticle> relevantArticles = searchResult.articles();

            if (relevantArticles.isEmpty()) {
                log.warn("No relevant articles found for question");
                return createNoResultsResponse();
            }

            // Step 3: Generate AI response with filtered context AND conversation memory
            String answer = generateAnswer(question, relevantArticles, conversationContext);

            // Step 4: Build citations (only from relevant articles)
            List<CitationDTO> citations = buildCitations(relevantArticles);

            log.info("Chat response generated with {} relevant citations (filtered from {} candidates)",
                    citations.size(), searchResult.metadata().originalCandidates());
            return new ChatResponse(answer, citations);

        } catch (Exception e) {
            // Re-throw exception - credit handling is now done at session level
            log.error("AI chat failed for user {}: {}", userId, e.getMessage());
            throw e;
        }
    }

    /**
     * Validate user question
     */
    private void validateQuestion(String question) {
        if (question == null || question.trim().isEmpty()) {
            throw new BadRequestException("Câu hỏi không được để trống");
        }

        if (question.length() > 500) {
            throw new BadRequestException("Câu hỏi quá dài (tối đa 500 ký tự)");
        }
    }

    /**
     * Generate AI answer with context from articles and conversation memory.
     * Delegates prompt construction to PromptBuilder.
     */
    private String generateAnswer(String question, List<LegalArticle> articles,
            ConversationContext conversationContext) {
        // Delegate prompt building to PromptBuilder (extracted for separation of
        // concerns)
        String prompt = promptBuilder.buildChatPrompt(question, articles, conversationContext);

        try {
            return aiService.generateText(prompt);
        } catch (Exception e) {
            log.error("Error calling AI service", e);
            throw new BadRequestException("Lỗi khi gọi AI: " + e.getMessage());
        }
    }

    /**
     * Build citations from articles
     */
    private List<CitationDTO> buildCitations(List<LegalArticle> articles) {
        return articles.stream()
                .limit(5) // Limit citations to top 5
                .map(this::createCitation)
                .collect(Collectors.toList());
    }

    /**
     * Create citation DTO from article
     */
    private CitationDTO createCitation(LegalArticle article) {
        return new CitationDTO(
                article.getId(),
                article.getDocument().getDocumentName(),
                article.getArticleNumber(),
                article.getArticleTitle(),
                truncate(article.getContent(), LegalSearchConfig.CITATION_PREVIEW_LENGTH));
    }

    /**
     * Create response when no articles found
     */
    private ChatResponse createNoResultsResponse() {
        String message = """
                Xin lỗi, tôi không tìm thấy thông tin liên quan trong cơ sở dữ liệu pháp luật hiện tại.

                Gợi ý:
                - Thử đặt câu hỏi khác với từ khóa rõ ràng hơn
                - Liên hệ chuyên gia pháp lý để được tư vấn chi tiết
                - Kiểm tra xem văn bản pháp luật liên quan đã được cập nhật chưa
                """;

        return new ChatResponse(message, List.of());
    }

    /**
     * Truncate text to maximum length
     */
    private String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }

        if (text.length() <= maxLength) {
            return text;
        }

        return text.substring(0, maxLength) + "...";
    }
}
