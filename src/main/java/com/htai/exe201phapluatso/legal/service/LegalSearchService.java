package com.htai.exe201phapluatso.legal.service;

import com.htai.exe201phapluatso.ai.service.OpenAIService;
import com.htai.exe201phapluatso.legal.config.LegalSearchConfig;
import com.htai.exe201phapluatso.legal.dto.ConversationContext;
import com.htai.exe201phapluatso.legal.entity.LegalArticle;
import com.htai.exe201phapluatso.legal.repo.LegalArticleRepo;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for searching relevant legal articles
 * Supports both keyword-based and vector-based (semantic) search
 */
@Service
public class LegalSearchService {

    private static final Logger log = LoggerFactory.getLogger(LegalSearchService.class);

    private final EntityManager entityManager;
    private final LegalArticleRepo articleRepo;
    private final VectorSearchService vectorSearchService;
    private final OpenAIService aiService;

    @Autowired
    public LegalSearchService(
            EntityManager entityManager,
            LegalArticleRepo articleRepo,
            VectorSearchService vectorSearchService,
            OpenAIService aiService) {
        this.entityManager = entityManager;
        this.articleRepo = articleRepo;
        this.vectorSearchService = vectorSearchService;
        this.aiService = aiService;
    }

    /**
     * Search for relevant legal articles using hybrid search (vector + keyword)
     * Falls back to keyword-only if vector search is not available
     * 
     * @param question User's question
     * @param limit    Maximum number of results
     * @return List of relevant articles, sorted by relevance score
     */
    public List<LegalArticle> searchRelevantArticles(String question, int limit) {
        if (question == null || question.trim().isEmpty()) {
            log.warn("Empty question provided to search");
            return Collections.emptyList();
        }

        // Try hybrid search first (vector + keyword)
        try {
            List<LegalArticle> results = vectorSearchService.hybridSearch(question, limit);
            if (!results.isEmpty()) {
                log.info("Hybrid search found {} results", results.size());
                return results;
            }
        } catch (Exception e) {
            log.warn("Hybrid search failed, falling back to keyword search: {}", e.getMessage());
        }

        // Fallback to keyword-only search
        return keywordSearch(question, limit);
    }

    /**
     * Keyword-only search (legacy method, used as fallback)
     */
    public List<LegalArticle> keywordSearch(String question, int limit) {
        // Extract keywords
        List<String> keywords = extractKeywords(question);
        log.info("Keyword search with: {}", keywords);

        if (keywords.isEmpty()) {
            log.warn("No keywords extracted from question: {}", question);
            return getFallbackArticles(limit);
        }

        // Search and score
        List<ArticleScore> scoredArticles = searchWithScoring(keywords);
        log.info("Found {} articles matching keywords", scoredArticles.size());

        // Return top N
        List<LegalArticle> results = scoredArticles.stream()
                .limit(limit)
                .map(ArticleScore::article)
                .collect(Collectors.toList());

        logTopResults(scoredArticles, 3);
        return results;
    }

    /**
     * Search articles matching keywords and calculate relevance scores
     * Filters by minimum relevance score and keyword match count
     */
    private List<ArticleScore> searchWithScoring(List<String> keywords) {
        // Find matching articles
        List<LegalArticle> articles = findArticlesByKeywords(keywords);

        // Calculate scores and filter by quality thresholds
        List<ArticleScore> scored = articles.stream()
                .map(article -> {
                    int score = calculateScore(article, keywords);
                    int matchCount = countKeywordMatches(article, keywords);
                    return new ArticleScore(article, score, matchCount);
                })
                // Filter: must meet minimum score AND minimum keyword matches
                .filter(as -> as.score() >= LegalSearchConfig.MIN_RELEVANCE_SCORE)
                .filter(as -> as.matchCount() >= Math.min(LegalSearchConfig.MIN_KEYWORD_MATCHES, keywords.size()))
                .sorted(Comparator.comparingInt(ArticleScore::score).reversed())
                .collect(Collectors.toList());

        log.info("Filtered from {} to {} articles (min score: {}, min matches: {})",
                articles.size(), scored.size(),
                LegalSearchConfig.MIN_RELEVANCE_SCORE, LegalSearchConfig.MIN_KEYWORD_MATCHES);

        return scored;
    }

    /**
     * Count how many distinct keywords match in the article
     */
    private int countKeywordMatches(LegalArticle article, List<String> keywords) {
        String content = normalize(article.getContent());
        String title = normalize(article.getArticleTitle());

        int matchCount = 0;
        for (String keyword : keywords) {
            String normalizedKeyword = keyword.toLowerCase();
            if (title.contains(normalizedKeyword) || content.contains(normalizedKeyword)) {
                matchCount++;
            }
        }
        return matchCount;
    }

    /**
     * Find articles matching any of the keywords using SQL LIKE
     */
    private List<LegalArticle> findArticlesByKeywords(List<String> keywords) {
        StringBuilder sql = new StringBuilder(
                "SELECT DISTINCT a.* FROM legal_articles a " +
                        "JOIN legal_documents d ON a.document_id = d.id " +
                        "WHERE d.status = 'Còn hiệu lực' AND (");

        // Build OR conditions for each keyword
        for (int i = 0; i < keywords.size(); i++) {
            if (i > 0)
                sql.append(" OR ");
            sql.append("(a.content LIKE :kw").append(i);
            sql.append(" OR a.article_title LIKE :kw").append(i).append(")");
        }
        sql.append(")");

        Query query = entityManager.createNativeQuery(sql.toString(), LegalArticle.class);

        // Bind parameters
        for (int i = 0; i < keywords.size(); i++) {
            query.setParameter("kw" + i, "%" + keywords.get(i) + "%");
        }

        @SuppressWarnings("unchecked")
        List<LegalArticle> results = query.getResultList();
        return results;
    }

    /**
     * Calculate relevance score for an article
     * Title matches are weighted higher than content matches
     */
    private int calculateScore(LegalArticle article, List<String> keywords) {
        String content = normalize(article.getContent());
        String title = normalize(article.getArticleTitle());

        int score = 0;
        for (String keyword : keywords) {
            String normalizedKeyword = keyword.toLowerCase();

            // Title matches (higher weight)
            score += countOccurrences(title, normalizedKeyword)
                    * LegalSearchConfig.TITLE_MATCH_WEIGHT;

            // Content matches
            score += countOccurrences(content, normalizedKeyword)
                    * LegalSearchConfig.CONTENT_MATCH_WEIGHT;
        }

        return score;
    }

    /**
     * Count occurrences of substring in text
     */
    private int countOccurrences(String text, String substring) {
        if (text.isEmpty() || substring.isEmpty()) {
            return 0;
        }

        int count = 0;
        int index = 0;

        while ((index = text.indexOf(substring, index)) != -1) {
            count++;
            index += substring.length();
        }

        return count;
    }

    /**
     * Extract keywords from question
     * Removes stop words and extracts meaningful terms
     */
    private List<String> extractKeywords(String question) {
        // Tokenize
        String[] words = question.toLowerCase()
                .replaceAll("[^\\p{L}\\s]", " ")
                .trim()
                .split("\\s+");

        // Filter and collect keywords
        List<String> keywords = Arrays.stream(words)
                .filter(word -> word.length() >= LegalSearchConfig.MIN_KEYWORD_LENGTH)
                .filter(word -> !LegalSearchConfig.STOP_WORDS.contains(word))
                .distinct()
                .limit(LegalSearchConfig.MAX_KEYWORDS)
                .collect(Collectors.toList());

        // Add bigrams for better context matching
        keywords.addAll(extractBigrams(words));

        return keywords;
    }

    /**
     * Extract 2-word phrases (bigrams) for better context
     */
    private List<String> extractBigrams(String[] words) {
        List<String> bigrams = new ArrayList<>();

        for (int i = 0; i < words.length - 1; i++) {
            String word1 = words[i];
            String word2 = words[i + 1];

            // Skip if either is a stop word or too short
            if (LegalSearchConfig.STOP_WORDS.contains(word1) ||
                    LegalSearchConfig.STOP_WORDS.contains(word2)) {
                continue;
            }

            if (word1.length() < LegalSearchConfig.MIN_KEYWORD_LENGTH ||
                    word2.length() < LegalSearchConfig.MIN_KEYWORD_LENGTH) {
                continue;
            }

            bigrams.add(word1 + " " + word2);
        }

        return bigrams.stream()
                .limit(LegalSearchConfig.MAX_BIGRAMS)
                .collect(Collectors.toList());
    }

    /**
     * Get fallback articles when no keywords found
     */
    private List<LegalArticle> getFallbackArticles(int limit) {
        log.info("Using fallback: returning recent articles");
        // Use paginated query instead of findAll() to avoid loading all articles
        return articleRepo.findRecentArticles(org.springframework.data.domain.PageRequest.of(0, limit));
    }

    /**
     * Normalize text for comparison (lowercase, handle null)
     */
    private String normalize(String text) {
        return text != null ? text.toLowerCase() : "";
    }

    /**
     * Unified search method for chat functionality
     * Combines query enhancement, multi-strategy search, and AI re-ranking
     */
    public SearchResult searchForChat(String question, int limit, ConversationContext context) {
        // Enhance query with conversation context
        String enhancedQuery = enhanceQueryWithContext(question, context);

        // Try hybrid search first (vector + keyword)
        List<LegalArticle> candidates = searchRelevantArticles(enhancedQuery, limit);

        // Apply AI re-ranking if we have enough candidates
        List<LegalArticle> finalResults = aiReRankArticles(question, candidates);

        // Create metadata
        SearchMetadata metadata = new SearchMetadata(
            true, // usedVector (assuming hybrid search includes vector)
            finalResults.size() < candidates.size(), // usedAiRerank
            candidates.size() // originalCandidates
        );

        return new SearchResult(finalResults, metadata);
    }

    /**
     * Build search query combining current question with conversation context
     * This helps find more relevant articles when user asks follow-up questions
     */
    public String enhanceQueryWithContext(String question, ConversationContext context) {
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
     * AI-powered re-ranking: Let AI analyze and select truly relevant articles
     * This filters out articles that match keywords but aren't actually relevant
     */
    public List<LegalArticle> aiReRankArticles(String question, List<LegalArticle> candidates) {
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
     * Log top search results for debugging
     */
    private void logTopResults(List<ArticleScore> scored, int count) {
        if (scored.isEmpty()) {
            return;
        }

        log.info("Top {} search results:", Math.min(count, scored.size()));
        for (int i = 0; i < Math.min(count, scored.size()); i++) {
            ArticleScore as = scored.get(i);
            log.info("  {}. Điều {} (score: {}) - {}",
                    i + 1,
                    as.article().getArticleNumber(),
                    as.score(),
                    as.article().getArticleTitle());
        }
    }

    /**
     * Result of unified search operation
     */
    public record SearchResult(List<LegalArticle> articles, SearchMetadata metadata) {}

    /**
     * Metadata about the search operation performed
     */
    public record SearchMetadata(boolean usedVector, boolean usedAiRerank, int originalCandidates) {}

    /**
     * Internal record to hold article with its relevance score and keyword match
     * count
     */
    private record ArticleScore(LegalArticle article, int score, int matchCount) {
    }
}
