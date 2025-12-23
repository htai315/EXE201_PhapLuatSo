package com.htai.exe201phapluatso.legal.service;

import com.htai.exe201phapluatso.ai.service.OpenAIService;
import com.htai.exe201phapluatso.common.exception.BadRequestException;
import com.htai.exe201phapluatso.legal.config.LegalSearchConfig;
import com.htai.exe201phapluatso.legal.dto.ChatResponse;
import com.htai.exe201phapluatso.legal.dto.CitationDTO;
import com.htai.exe201phapluatso.legal.entity.LegalArticle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for RAG-based legal chatbot
 * Implements Retrieval-Augmented Generation pattern
 */
@Service
public class LegalChatService {

    private static final Logger log = LoggerFactory.getLogger(LegalChatService.class);

    private final LegalSearchService searchService;
    private final OpenAIService aiService;

    public LegalChatService(LegalSearchService searchService, OpenAIService aiService) {
        this.searchService = searchService;
        this.aiService = aiService;
    }

    /**
     * Process user question using RAG pipeline
     * 
     * @param question User's legal question
     * @return AI-generated answer with citations
     */
    public ChatResponse chat(String question) {
        validateQuestion(question);
        
        log.info("Processing chat question: {}", question);

        // Step 1: Retrieve relevant articles
        List<LegalArticle> articles = retrieveRelevantArticles(question);
        
        if (articles.isEmpty()) {
            log.warn("No relevant articles found for question");
            return createNoResultsResponse();
        }

        // Step 2: Generate AI response with context
        String answer = generateAnswer(question, articles);

        // Step 3: Build citations
        List<CitationDTO> citations = buildCitations(articles);

        log.info("Chat response generated successfully with {} citations", citations.size());
        return new ChatResponse(answer, citations);
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
            LegalSearchConfig.DEFAULT_SEARCH_LIMIT
        );
    }

    /**
     * Generate AI answer with context from articles
     */
    private String generateAnswer(String question, List<LegalArticle> articles) {
        String context = buildContext(articles);
        String prompt = buildPrompt(question, context);
        
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
     * Build AI prompt with instructions and context
     */
    private String buildPrompt(String question, String context) {
        return String.format("""
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
            
            CÂU HỎI:
            %s
            
            ĐIỀU LUẬT LIÊN QUAN:
            %s
            
            TRẢ LỜI (ngắn gọn, đúng trọng tâm):
            """, 
            question, 
            context
        );
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
            truncate(article.getContent(), LegalSearchConfig.CITATION_PREVIEW_LENGTH)
        );
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
