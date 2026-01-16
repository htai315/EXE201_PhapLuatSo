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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for RAG-based legal chatbot
 * Implements Retrieval-Augmented Generation pattern with credits system
 * Supports both regular and streaming responses
 */
@Service
public class LegalChatService {

    private static final Logger log = LoggerFactory.getLogger(LegalChatService.class);

    private final LegalSearchService searchService;
    private final OpenAIService aiService;
    private final CreditService creditService;

    public LegalChatService(
            LegalSearchService searchService,
            OpenAIService aiService,
            CreditService creditService) {
        this.searchService = searchService;
        this.aiService = aiService;
        this.creditService = creditService;
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

        // STEP 0: Reserve credit BEFORE processing (will be refunded if AI fails)
        com.htai.exe201phapluatso.credit.entity.CreditReservation reservation = creditService.reserveCredit(userId,
                "CHAT", "AI_CHAT");

        log.info("Processing chat question for user {}: {}", userId, question);

        try {
            // Step 1: Retrieve candidate articles (more than needed)
            // Use conversation context to improve search if available
            String searchQuery = buildSearchQuery(question, conversationContext);
            List<LegalArticle> candidateArticles = retrieveRelevantArticles(searchQuery);

            if (candidateArticles.isEmpty()) {
                log.warn("No relevant articles found for question");
                // Confirm credit even if no results (search was performed)
                creditService.confirmReservation(reservation.getId());
                return createNoResultsResponse();
            }

            // Step 2: AI re-ranking - Let AI analyze and select most relevant articles
            List<LegalArticle> relevantArticles = aiReRankArticles(question, candidateArticles);

            if (relevantArticles.isEmpty()) {
                log.warn("AI determined no articles are truly relevant");
                creditService.confirmReservation(reservation.getId());
                return createNoResultsResponse();
            }

            // Step 3: Generate AI response with filtered context AND conversation memory
            String answer = generateAnswer(question, relevantArticles, conversationContext);

            // Step 4: Build citations (only from relevant articles)
            List<CitationDTO> citations = buildCitations(relevantArticles);

            // Confirm credit deduction on success
            creditService.confirmReservation(reservation.getId());

            log.info("Chat response generated with {} relevant citations (filtered from {} candidates)",
                    citations.size(), candidateArticles.size());
            return new ChatResponse(answer, citations);

        } catch (Exception e) {
            // Refund credit if AI operation fails
            log.error("AI chat failed, refunding credit for user {}: {}", userId, e.getMessage());
            creditService.refundReservation(reservation.getId());
            throw e;
        }
    }

    /**
     * Build search query combining current question with conversation context
     * This helps find more relevant articles when user asks follow-up questions
     */
    private String buildSearchQuery(String question, ConversationContext context) {
        if (context == null || context.isEmpty()) {
            return question;
        }

        // Extract key topics from recent conversation to enhance search
        StringBuilder queryBuilder = new StringBuilder(question);

        // Add keywords from last assistant response if it contains legal terms
        String lastAssistantMessage = context.getLastAssistantMessage();
        if (lastAssistantMessage != null) {
            // Extract "Điều X" references from previous response
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("Điều\\s+(\\d+)");
            java.util.regex.Matcher matcher = pattern.matcher(lastAssistantMessage);
            while (matcher.find()) {
                queryBuilder.append(" Điều ").append(matcher.group(1));
            }
        }

        return queryBuilder.toString();
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
     * Retrieve relevant articles using search service
     */
    private List<LegalArticle> retrieveRelevantArticles(String question) {
        return searchService.searchRelevantArticles(
                question,
                LegalSearchConfig.DEFAULT_SEARCH_LIMIT);
    }

    /**
     * AI-powered re-ranking: Let AI analyze and select truly relevant articles
     * This filters out articles that match keywords but aren't actually relevant
     */
    private List<LegalArticle> aiReRankArticles(String question, List<LegalArticle> candidates) {
        if (candidates.size() <= 3) {
            // If we have 3 or fewer, assume all are relevant
            return candidates;
        }

        log.info("AI re-ranking {} candidate articles", candidates.size());

        // Build analysis prompt
        String analysisPrompt = buildReRankingPrompt(question, candidates);

        try {
            String aiResponse = aiService.generateText(analysisPrompt);
            List<Integer> selectedIndices = parseSelectedIndices(aiResponse, candidates.size());

            List<LegalArticle> selected = selectedIndices.stream()
                    .filter(i -> i >= 0 && i < candidates.size())
                    .map(candidates::get)
                    .collect(Collectors.toList());

            log.info("AI selected {} out of {} articles as truly relevant", selected.size(), candidates.size());
            return selected.isEmpty() ? candidates.subList(0, Math.min(3, candidates.size())) : selected;

        } catch (Exception e) {
            log.error("Error in AI re-ranking, falling back to top 5", e);
            // Fallback: return top 5 if AI fails
            return candidates.subList(0, Math.min(5, candidates.size()));
        }
    }

    /**
     * Build prompt for AI to analyze and select relevant articles
     * Uses longer preview for better context understanding
     */
    private String buildReRankingPrompt(String question, List<LegalArticle> candidates) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("""
                Bạn là chuyên gia phân tích pháp luật với nhiệm vụ XÁC ĐỊNH điều luật nào THỰC SỰ LIÊN QUAN đến câu hỏi.

                CẢNH BÁO: Nhiều điều luật có thể chứa từ khóa giống nhau nhưng KHÔNG liên quan đến câu hỏi.
                Bạn phải phân biệt giữa:
                - Điều luật TRỰC TIẾP trả lời câu hỏi (CHỌN)
                - Điều luật chỉ chứa từ khóa tương tự nhưng về chủ đề khác (BỎ QUA)

                """);
        prompt.append("CÂU HỎI CỦA NGƯỜI DÙNG:\n");
        prompt.append(question).append("\n\n");
        prompt.append("CÁC ĐIỀU LUẬT ỨNG VIÊN:\n\n");

        for (int i = 0; i < candidates.size(); i++) {
            LegalArticle article = candidates.get(i);
            prompt.append(String.format("[%d] Điều %d", i, article.getArticleNumber()));
            if (article.getArticleTitle() != null && !article.getArticleTitle().isEmpty()) {
                prompt.append(" - ").append(article.getArticleTitle());
            }
            prompt.append("\n");
            prompt.append("Văn bản: ").append(article.getDocument().getDocumentName()).append("\n");
            // Use longer preview for better context understanding
            String preview = article.getContent().length() > LegalSearchConfig.RERANK_PREVIEW_LENGTH
                    ? article.getContent().substring(0, LegalSearchConfig.RERANK_PREVIEW_LENGTH) + "..."
                    : article.getContent();
            prompt.append("Nội dung: ").append(preview).append("\n\n");
        }

        prompt.append("""
                TIÊU CHÍ ĐÁNH GIÁ (áp dụng nghiêm ngặt):
                ✅ CHỌN nếu điều luật:
                   - Quy định TRỰC TIẾP về vấn đề người dùng hỏi
                   - Chứa thông tin CỤ THỂ để trả lời câu hỏi (số liệu, điều kiện, quy trình...)
                   - Thuộc ĐÚNG lĩnh vực pháp luật mà câu hỏi đề cập

                ❌ BỎ QUA nếu điều luật:
                   - Chỉ chứa từ khóa giống nhưng về CHỦ ĐỀ KHÁC
                   - Là quy định chung/nguyên tắc mà không trả lời được câu hỏi cụ thể
                   - Thuộc lĩnh vực pháp luật khác (VD: hỏi về hôn nhân nhưng điều luật về lao động)

                YÊU CẦU:
                - Tối đa 3-5 điều THỰC SỰ liên quan
                - Ưu tiên CHÍNH XÁC hơn ĐA DẠNG
                - Nếu không có điều nào phù hợp, trả về: NONE

                TRẢ LỜI (chỉ ghi số thứ tự, cách nhau bởi dấu phẩy):
                VD: 0,2,5 hoặc NONE
                """);

        return prompt.toString();
    }

    /**
     * Parse AI response to extract selected article indices
     */
    private List<Integer> parseSelectedIndices(String aiResponse, int maxIndex) {
        if (aiResponse == null || aiResponse.trim().isEmpty()) {
            return List.of();
        }

        String cleaned = aiResponse.trim().toUpperCase();

        // Check if AI said NONE
        if (cleaned.contains("NONE") || cleaned.contains("KHÔNG CÓ")) {
            return List.of();
        }

        // Extract numbers
        List<Integer> indices = new ArrayList<>();
        String[] parts = cleaned.split("[,\\s]+");

        for (String part : parts) {
            try {
                // Remove any non-digit characters
                String digits = part.replaceAll("[^0-9]", "");
                if (!digits.isEmpty()) {
                    int index = Integer.parseInt(digits);
                    if (index >= 0 && index < maxIndex) {
                        indices.add(index);
                    }
                }
            } catch (NumberFormatException e) {
                // Skip invalid numbers
            }
        }

        return indices;
    }

    /**
     * Generate AI answer with context from articles and conversation memory
     */
    private String generateAnswer(String question, List<LegalArticle> articles,
            ConversationContext conversationContext) {
        String context = buildContext(articles);
        String prompt = buildPromptWithMemory(question, context, conversationContext);

        try {
            return aiService.generateText(prompt);
        } catch (Exception e) {
            log.error("Error calling AI service", e);
            throw new BadRequestException("Lỗi khi gọi AI: " + e.getMessage());
        }
    }

    /**
     * Build context string from articles
     */
    private String buildContext(List<LegalArticle> articles) {
        StringBuilder context = new StringBuilder();

        for (int i = 0; i < articles.size(); i++) {
            LegalArticle article = articles.get(i);

            context.append("--- Điều luật ").append(i + 1).append(" ---\n");
            context.append("Văn bản: ").append(article.getDocument().getDocumentName()).append("\n");
            context.append("Điều ").append(article.getArticleNumber());

            if (article.getArticleTitle() != null && !article.getArticleTitle().isEmpty()) {
                context.append(". ").append(article.getArticleTitle());
            }

            context.append("\n\n");
            context.append(article.getContent());
            context.append("\n\n");
        }

        return context.toString();
    }

    /**
     * Build AI prompt with conversation memory for context-aware responses
     */
    private String buildPromptWithMemory(String question, String context, ConversationContext conversationContext) {
        StringBuilder promptBuilder = new StringBuilder();

        promptBuilder.append("""
                Bạn là chuyên gia tư vấn pháp luật Việt Nam.

                HƯỚNG DẪN TRẢ LỜI:
                1. ĐỌC KỸ câu hỏi để hiểu chính xác người dùng muốn biết gì
                2. PHÂN TÍCH các điều luật được cung cấp để tìm thông tin liên quan TRỰC TIẾP đến câu hỏi
                3. TRẢ LỜI NGẮN GỌN, chỉ tập trung vào điểm chính mà người dùng hỏi
                4. TRÍCH DẪN điều luật cụ thể (VD: "Theo Điều 123 Bộ luật Dân sự...")

                QUY TẮC BẮT BUỘC:
                - Chỉ trả lời ĐÚNG TRỌNG TÂM câu hỏi, KHÔNG giải thích thêm những gì không được hỏi
                - Trả lời TỐI ĐA 3-4 câu ngắn gọn (khoảng 100-150 từ)
                - Nếu câu hỏi đơn giản (VD: "Tuổi kết hôn là bao nhiêu?"), chỉ cần 1-2 câu trả lời
                - LUÔN trích dẫn điều luật trong câu trả lời
                - KHÔNG viết dài dòng, KHÔNG giải thích thêm nếu không được hỏi
                - Nếu thông tin không đủ, nói ngắn gọn: "Các điều luật trên chưa đủ thông tin để trả lời đầy đủ"

                CẤU TRÚC TRẢ LỜI:
                - Câu 1: Trả lời trực tiếp câu hỏi + trích dẫn điều luật
                - Câu 2-3: Giải thích ngắn gọn (nếu cần thiết)
                - KHÔNG thêm phần kết luận, lời khuyên nếu không được hỏi
                """);

        // Add conversation history if available
        if (conversationContext != null && !conversationContext.isEmpty()) {
            promptBuilder.append("\n\nLỊCH SỬ HỘI THOẠI (để hiểu ngữ cảnh):\n");
            for (ConversationContext.Message msg : conversationContext.getMessages()) {
                String role = msg.role().equals("USER") ? "Người dùng" : "Trợ lý";
                // Truncate long messages to save tokens
                String content = msg.content().length() > 300
                        ? msg.content().substring(0, 300) + "..."
                        : msg.content();
                promptBuilder.append(role).append(": ").append(content).append("\n");
            }
            promptBuilder.append("\nLƯU Ý: Hãy xem xét ngữ cảnh từ lịch sử hội thoại khi trả lời. ");
            promptBuilder.append(
                    "Nếu người dùng hỏi \"nó\", \"điều đó\", \"vấn đề này\"... hãy hiểu họ đang đề cập đến chủ đề trước đó.\n");
        }

        promptBuilder.append("\nCÂU HỎI HIỆN TẠI:\n");
        promptBuilder.append(question);
        promptBuilder.append("\n\nĐIỀU LUẬT LIÊN QUAN:\n");
        promptBuilder.append(context);
        promptBuilder.append("\n\nTRẢ LỜI (ngắn gọn, đúng trọng tâm):\n");

        return promptBuilder.toString();
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
