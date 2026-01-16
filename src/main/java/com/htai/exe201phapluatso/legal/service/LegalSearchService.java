package com.htai.exe201phapluatso.legal.service;

import com.htai.exe201phapluatso.legal.config.LegalSearchConfig;
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

    @Autowired
    public LegalSearchService(
            EntityManager entityManager,
            LegalArticleRepo articleRepo,
            VectorSearchService vectorSearchService) {
        this.entityManager = entityManager;
        this.articleRepo = articleRepo;
        this.vectorSearchService = vectorSearchService;
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
     * Internal record to hold article with its relevance score and keyword match
     * count
     */
    private record ArticleScore(LegalArticle article, int score, int matchCount) {
    }
}
