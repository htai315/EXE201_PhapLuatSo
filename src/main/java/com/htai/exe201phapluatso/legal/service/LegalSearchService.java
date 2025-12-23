package com.htai.exe201phapluatso.legal.service;

import com.htai.exe201phapluatso.legal.config.LegalSearchConfig;
import com.htai.exe201phapluatso.legal.entity.LegalArticle;
import com.htai.exe201phapluatso.legal.repo.LegalArticleRepo;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for searching relevant legal articles
 * Uses keyword extraction and scoring algorithm
 */
@Service
public class LegalSearchService {

    private static final Logger log = LoggerFactory.getLogger(LegalSearchService.class);

    private final EntityManager entityManager;
    private final LegalArticleRepo articleRepo;

    public LegalSearchService(EntityManager entityManager, LegalArticleRepo articleRepo) {
        this.entityManager = entityManager;
        this.articleRepo = articleRepo;
    }

    /**
     * Search for relevant legal articles
     * 
     * @param question User's question
     * @param limit Maximum number of results
     * @return List of relevant articles, sorted by relevance score
     */
    public List<LegalArticle> searchRelevantArticles(String question, int limit) {
        if (question == null || question.trim().isEmpty()) {
            log.warn("Empty question provided to search");
            return Collections.emptyList();
        }

        // Extract keywords
        List<String> keywords = extractKeywords(question);
        log.info("Search keywords extracted: {}", keywords);
        
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
     */
    private List<ArticleScore> searchWithScoring(List<String> keywords) {
        // Find matching articles
        List<LegalArticle> articles = findArticlesByKeywords(keywords);
        
        // Calculate scores
        List<ArticleScore> scored = articles.stream()
                .map(article -> new ArticleScore(article, calculateScore(article, keywords)))
                .filter(as -> as.score() > 0)
                .sorted(Comparator.comparingInt(ArticleScore::score).reversed())
                .collect(Collectors.toList());
        
        return scored;
    }

    /**
     * Find articles matching any of the keywords using SQL LIKE
     */
    private List<LegalArticle> findArticlesByKeywords(List<String> keywords) {
        StringBuilder sql = new StringBuilder(
            "SELECT DISTINCT a.* FROM legal_articles a " +
            "JOIN legal_documents d ON a.document_id = d.id " +
            "WHERE d.status = N'Còn hiệu lực' AND ("
        );

        // Build OR conditions for each keyword
        for (int i = 0; i < keywords.size(); i++) {
            if (i > 0) sql.append(" OR ");
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
        return articleRepo.findAll().stream()
                .limit(limit)
                .collect(Collectors.toList());
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
                as.article().getArticleTitle()
            );
        }
    }

    /**
     * Internal record to hold article with its relevance score
     */
    private record ArticleScore(LegalArticle article, int score) {}
}
