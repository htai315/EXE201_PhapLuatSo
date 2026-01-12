package com.htai.exe201phapluatso.legal.service;

import com.htai.exe201phapluatso.ai.service.EmbeddingService;
import com.htai.exe201phapluatso.legal.config.LegalSearchConfig;
import com.htai.exe201phapluatso.legal.entity.LegalArticle;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for vector-based semantic search using pgvector
 * Provides hybrid search combining vector similarity and keyword matching
 */
@Service
public class VectorSearchService {

    private static final Logger log = LoggerFactory.getLogger(VectorSearchService.class);

    private final EntityManager entityManager;
    private final EmbeddingService embeddingService;

    // Search configuration
    private static final float SIMILARITY_THRESHOLD = 0.25f;
    private static final float VECTOR_WEIGHT = 0.7f;
    private static final float KEYWORD_WEIGHT = 0.3f;

    public VectorSearchService(EntityManager entityManager, EmbeddingService embeddingService) {
        this.entityManager = entityManager;
        this.embeddingService = embeddingService;
    }

    /**
     * Semantic search using vector similarity
     * Falls back to keyword search if no embeddings available
     */
    public List<LegalArticle> semanticSearch(String question, int limit) {
        log.info("Performing semantic search for: {}", truncateForLog(question));

        // Check if we have embeddings
        long embeddingCount = countArticlesWithEmbeddings();
        if (embeddingCount == 0) {
            log.warn("No embeddings found, falling back to keyword search");
            return keywordOnlySearch(question, limit);
        }

        try {
            // Generate embedding for the question
            float[] questionEmbedding = embeddingService.generateEmbedding(question);
            String vectorString = embeddingService.toVectorString(questionEmbedding);

            // Perform vector search
            List<LegalArticle> results = vectorSearch(vectorString, limit);
            
            if (results.isEmpty()) {
                log.info("Vector search returned no results, trying keyword search");
                return keywordOnlySearch(question, limit);
            }

            log.info("Semantic search found {} results", results.size());
            return results;

        } catch (Exception e) {
            log.error("Error in semantic search, falling back to keyword: {}", e.getMessage());
            return keywordOnlySearch(question, limit);
        }
    }

    /**
     * Hybrid search combining vector similarity and keyword matching
     */
    public List<LegalArticle> hybridSearch(String question, int limit) {
        log.info("Performing hybrid search for: {}", truncateForLog(question));

        long embeddingCount = countArticlesWithEmbeddings();
        if (embeddingCount == 0) {
            log.warn("No embeddings found, using keyword-only search");
            return keywordOnlySearch(question, limit);
        }

        try {
            // Generate embedding
            float[] questionEmbedding = embeddingService.generateEmbedding(question);
            String vectorString = embeddingService.toVectorString(questionEmbedding);

            // Extract keywords
            List<String> keywords = extractKeywords(question);
            
            // Perform hybrid search
            List<LegalArticle> results = performHybridSearch(vectorString, keywords, limit);
            
            if (results.isEmpty()) {
                log.info("Hybrid search returned no results, trying keyword-only");
                return keywordOnlySearch(question, limit);
            }

            log.info("Hybrid search found {} results", results.size());
            return results;

        } catch (Exception e) {
            log.error("Error in hybrid search: {}", e.getMessage());
            return keywordOnlySearch(question, limit);
        }
    }

    /**
     * Pure vector similarity search
     */
    @SuppressWarnings("unchecked")
    private List<LegalArticle> vectorSearch(String vectorString, int limit) {
        String sql = """
            SELECT a.* FROM legal_articles a
            JOIN legal_documents d ON a.document_id = d.id
            WHERE a.embedding IS NOT NULL
              AND d.status = 'Còn hiệu lực'
              AND 1 - (a.embedding <=> CAST(:vector AS vector)) >= :threshold
            ORDER BY a.embedding <=> CAST(:vector AS vector)
            LIMIT :limit
            """;

        Query query = entityManager.createNativeQuery(sql, LegalArticle.class);
        query.setParameter("vector", vectorString);
        query.setParameter("threshold", SIMILARITY_THRESHOLD);
        query.setParameter("limit", limit);

        return query.getResultList();
    }

    /**
     * Hybrid search using both vector and keywords
     */
    @SuppressWarnings("unchecked")
    private List<LegalArticle> performHybridSearch(String vectorString, List<String> keywords, int limit) {
        // Build dynamic SQL for hybrid search
        StringBuilder sql = new StringBuilder();
        sql.append("""
            WITH scored_articles AS (
                SELECT 
                    a.*,
                    CASE 
                        WHEN a.embedding IS NOT NULL 
                        THEN 1 - (a.embedding <=> CAST(:vector AS vector))
                        ELSE 0 
                    END AS vector_score,
                    (
            """);

        // Build keyword scoring
        if (!keywords.isEmpty()) {
            List<String> keywordScores = new ArrayList<>();
            for (int i = 0; i < keywords.size(); i++) {
                keywordScores.add(String.format(
                    "(CASE WHEN a.article_title ILIKE :kw%d THEN 3 ELSE 0 END + " +
                    "CASE WHEN a.content ILIKE :kw%d THEN 1 ELSE 0 END)", i, i));
            }
            sql.append(String.join(" + ", keywordScores));
        } else {
            sql.append("0");
        }

        sql.append("""
                    )::FLOAT / GREATEST(:kwCount, 1) AS keyword_score
                FROM legal_articles a
                JOIN legal_documents d ON a.document_id = d.id
                WHERE d.status = 'Còn hiệu lực'
            )
            SELECT * FROM scored_articles
            WHERE vector_score > 0.15 OR keyword_score > 0
            ORDER BY (vector_score * :vWeight + keyword_score * :kWeight) DESC
            LIMIT :limit
            """);

        Query query = entityManager.createNativeQuery(sql.toString(), LegalArticle.class);
        query.setParameter("vector", vectorString);
        query.setParameter("vWeight", VECTOR_WEIGHT);
        query.setParameter("kWeight", KEYWORD_WEIGHT);
        query.setParameter("kwCount", Math.max(keywords.size(), 1));
        query.setParameter("limit", limit);

        // Bind keyword parameters
        for (int i = 0; i < keywords.size(); i++) {
            query.setParameter("kw" + i, "%" + keywords.get(i) + "%");
        }

        return query.getResultList();
    }

    /**
     * Keyword-only search (fallback)
     */
    @SuppressWarnings("unchecked")
    private List<LegalArticle> keywordOnlySearch(String question, int limit) {
        List<String> keywords = extractKeywords(question);
        
        if (keywords.isEmpty()) {
            log.warn("No keywords extracted from question");
            return List.of();
        }

        StringBuilder sql = new StringBuilder();
        sql.append("""
            SELECT DISTINCT a.* FROM legal_articles a
            JOIN legal_documents d ON a.document_id = d.id
            WHERE d.status = 'Còn hiệu lực' AND (
            """);

        List<String> conditions = new ArrayList<>();
        for (int i = 0; i < keywords.size(); i++) {
            conditions.add(String.format(
                "(a.content ILIKE :kw%d OR a.article_title ILIKE :kw%d)", i, i));
        }
        sql.append(String.join(" OR ", conditions));
        sql.append(") LIMIT :limit");

        Query query = entityManager.createNativeQuery(sql.toString(), LegalArticle.class);
        for (int i = 0; i < keywords.size(); i++) {
            query.setParameter("kw" + i, "%" + keywords.get(i) + "%");
        }
        query.setParameter("limit", limit);

        return query.getResultList();
    }

    /**
     * Generate and save embedding for an article
     */
    @Transactional
    public void generateAndSaveEmbedding(Long articleId) {
        LegalArticle article = entityManager.find(LegalArticle.class, articleId);
        if (article == null) {
            log.warn("Article not found: {}", articleId);
            return;
        }

        try {
            float[] embedding = embeddingService.generateArticleEmbedding(
                article.getArticleTitle(), 
                article.getContent()
            );
            String vectorString = embeddingService.toVectorString(embedding);

            // Update using native query (JPA doesn't support vector type directly)
            String sql = """
                UPDATE legal_articles 
                SET embedding = CAST(:vector AS vector), 
                    embedding_updated_at = :updatedAt
                WHERE id = :id
                """;
            
            entityManager.createNativeQuery(sql)
                .setParameter("vector", vectorString)
                .setParameter("updatedAt", LocalDateTime.now())
                .setParameter("id", articleId)
                .executeUpdate();

            log.info("Generated embedding for article {}", articleId);

        } catch (Exception e) {
            log.error("Failed to generate embedding for article {}: {}", articleId, e.getMessage());
        }
    }

    /**
     * Batch generate embeddings for articles without embeddings
     */
    @Transactional
    public int generateMissingEmbeddings(int batchSize) {
        @SuppressWarnings("unchecked")
        List<Long> articleIds = entityManager.createNativeQuery(
            "SELECT id FROM legal_articles WHERE embedding IS NULL LIMIT :limit")
            .setParameter("limit", batchSize)
            .getResultList();

        int count = 0;
        for (Object idObj : articleIds) {
            Long id = ((Number) idObj).longValue();
            try {
                generateAndSaveEmbedding(id);
                count++;
                // Small delay to avoid rate limiting
                Thread.sleep(100);
            } catch (Exception e) {
                log.error("Error generating embedding for article {}: {}", id, e.getMessage());
            }
        }

        log.info("Generated {} embeddings in this batch", count);
        return count;
    }

    /**
     * Count articles with embeddings
     */
    public long countArticlesWithEmbeddings() {
        return ((Number) entityManager.createNativeQuery(
            "SELECT COUNT(*) FROM legal_articles WHERE embedding IS NOT NULL")
            .getSingleResult()).longValue();
    }

    /**
     * Count articles without embeddings
     */
    public long countArticlesWithoutEmbeddings() {
        return ((Number) entityManager.createNativeQuery(
            "SELECT COUNT(*) FROM legal_articles WHERE embedding IS NULL")
            .getSingleResult()).longValue();
    }

    /**
     * Extract keywords from question
     */
    private List<String> extractKeywords(String question) {
        String[] words = question.toLowerCase()
            .replaceAll("[^\\p{L}\\s]", " ")
            .trim()
            .split("\\s+");

        return Arrays.stream(words)
            .filter(word -> word.length() >= LegalSearchConfig.MIN_KEYWORD_LENGTH)
            .filter(word -> !LegalSearchConfig.STOP_WORDS.contains(word))
            .distinct()
            .limit(LegalSearchConfig.MAX_KEYWORDS)
            .collect(Collectors.toList());
    }

    private String truncateForLog(String text) {
        return text.length() > 100 ? text.substring(0, 100) + "..." : text;
    }
}
