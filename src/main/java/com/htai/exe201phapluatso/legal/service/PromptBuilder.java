package com.htai.exe201phapluatso.legal.service;

import com.htai.exe201phapluatso.legal.config.LegalSearchConfig;
import com.htai.exe201phapluatso.legal.dto.ConversationContext;
import com.htai.exe201phapluatso.legal.entity.LegalArticle;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Responsible for building prompts for AI chat responses.
 * Extracted from LegalChatService for better separation of concerns.
 * 
 * Single responsibility: Construct well-formatted prompts with:
 * - Legal context from articles
 * - Conversation memory
 * - Structured instructions for AI
 */
@Component
public class PromptBuilder {

    /**
     * Build complete prompt with conversation memory for AI response generation.
     * 
     * @param question            Current user question
     * @param articles            Relevant legal articles for context
     * @param conversationContext Previous messages in session (can be null)
     * @return Formatted prompt string for AI
     */
    public String buildChatPrompt(String question, List<LegalArticle> articles,
            ConversationContext conversationContext) {
        String context = buildArticlesContext(articles);
        return buildPromptWithMemory(question, context, conversationContext);
    }

    /**
     * Build context string from legal articles.
     * Format: Each article with document name, article number, title, and content.
     */
    String buildArticlesContext(List<LegalArticle> articles) {
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
     * Build AI prompt with conversation memory for context-aware responses.
     * Includes: base instructions, conversation history, current question, and
     * legal context.
     */
    private String buildPromptWithMemory(String question, String articlesContext,
            ConversationContext conversationContext) {
        StringBuilder promptBuilder = new StringBuilder();

        // Base prompt with structured instructions
        String basePrompt = String.format(
                """
                        Bạn là chuyên gia tư vấn pháp luật Việt Nam, làm việc như một luật sư/tư vấn viên chuyên sâu.

                        HƯỚNG DẪN TRẢ LỜI (bắt buộc thực hiện theo cấu trúc):
                        1. ĐỌC KỸ câu hỏi để hiểu chính xác phạm vi người dùng muốn biết.
                        2. DÙNG các điều luật được cung cấp làm nguồn chính để trả lời; trích dẫn cụ thể (ví dụ: "Theo Điều 123 Bộ luật Dân sự...").
                        3. PHÂN TÍCH logic pháp lý: nêu điều kiện, điều khoản áp dụng và lý do dẫn đến kết luận.
                        4. KẾT LUẬN rõ ràng và, nếu phù hợp, đưa ra gợi ý hành động ngắn (ví dụ: thủ tục, tài liệu cần chuẩn bị).

                        QUY TẮC BẮT BUỘC:
                        - Trả lời phải có cấu trúc: (A) Câu trả lời trực tiếp, (B) Phân tích chi tiết với trích dẫn, (C) Gợi ý/ hệ quả pháp lý, (D) Tóm tắt 1 câu.
                        - Độ dài mong muốn: khoảng 150 - %d từ (ưu tiên rõ ràng và logic hơn quá ngắn).
                        - LUÔN trích dẫn điều luật cụ thể ngay trong phần phân tích khi sử dụng nội dung pháp luật.
                        - Nếu thông tin từ các điều luật không đủ để kết luận, hãy nêu rõ điểm thiếu và đề xuất bước tiếp theo để thu thập thông tin.

                        YÊU CẦU VỀ GIỌNG VĂN:
                        - Trung lập, trang trọng, dễ hiểu với người không chuyên.
                        - Sử dụng bullet/đoạn ngắn khi liệt kê điều kiện hoặc bước hành động.

                        CẤU TRÚC KẾT QUẢ (ví dụ):
                        1) Trả lời trực tiếp: 1-2 câu.
                        2) Phân tích: 3-6 câu kèm trích dẫn điều luật.
                        3) Gợi ý/Hành động: 1-3 câu (nếu có).
                        4) Tóm tắt: 1 câu ngắn gọn.
                        """,
                LegalSearchConfig.MAX_ANSWER_WORDS);

        promptBuilder.append(basePrompt);

        // Add conversation history if available
        if (conversationContext != null && !conversationContext.isEmpty()) {
            promptBuilder.append("\n\nLỊCH SỬ HỘI THOẠI (để hiểu ngữ cảnh):\n");
            for (ConversationContext.Message msg : conversationContext.getMessages()) {
                String role = "USER".equals(msg.role()) ? "Người dùng" : "Trợ lý";

                String msgContent = msg.content() == null ? "" : msg.content();
                String content = msgContent.length() > 300
                        ? msgContent.substring(0, 300) + "..."
                        : msgContent;

                promptBuilder.append(role).append(": ").append(content).append("\n");
            }
            promptBuilder.append("\nLƯU Ý: Hãy xem xét ngữ cảnh từ lịch sử hội thoại khi trả lời. ");
            promptBuilder.append(
                    "Nếu người dùng hỏi \"nó\", \"điều đó\", \"vấn đề này\"... hãy hiểu họ đang đề cập đến chủ đề trước đó.\n");
        }

        promptBuilder.append("\nCÂU HỎI HIỆN TẠI:\n");
        promptBuilder.append(question);

        promptBuilder.append("\n\nĐIỀU LUẬT LIÊN QUAN:\n");
        promptBuilder.append(articlesContext);

        promptBuilder.append("\n\nTRẢ LỜI (ngắn gọn, đúng trọng tâm):\n");

        return promptBuilder.toString();
    }
}
