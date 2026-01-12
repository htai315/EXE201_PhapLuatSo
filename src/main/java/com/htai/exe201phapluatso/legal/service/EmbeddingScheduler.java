package com.htai.exe201phapluatso.legal.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled task to automatically generate embeddings for new articles
 * Runs periodically to ensure all articles have embeddings for semantic search
 */
@Component
public class EmbeddingScheduler {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingScheduler.class);

    private final VectorSearchService vectorSearchService;

    @Value("${embedding.auto-generate.enabled:true}")
    private boolean autoGenerateEnabled;

    @Value("${embedding.auto-generate.batch-size:10}")
    private int batchSize;

    public EmbeddingScheduler(VectorSearchService vectorSearchService) {
        this.vectorSearchService = vectorSearchService;
    }

    /**
     * Generate embeddings for articles without embeddings
     * Runs every 5 minutes
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void generateMissingEmbeddings() {
        if (!autoGenerateEnabled) {
            return;
        }

        long missing = vectorSearchService.countArticlesWithoutEmbeddings();
        if (missing == 0) {
            return; // Nothing to do
        }

        log.info("Auto-generating embeddings: {} articles missing embeddings", missing);
        
        try {
            int generated = vectorSearchService.generateMissingEmbeddings(batchSize);
            if (generated > 0) {
                log.info("Auto-generated {} embeddings, {} remaining", 
                    generated, vectorSearchService.countArticlesWithoutEmbeddings());
            }
        } catch (Exception e) {
            log.error("Error in auto-generate embeddings task: {}", e.getMessage());
        }
    }

    /**
     * Log embedding statistics daily at midnight
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void logEmbeddingStats() {
        long withEmbeddings = vectorSearchService.countArticlesWithEmbeddings();
        long withoutEmbeddings = vectorSearchService.countArticlesWithoutEmbeddings();
        long total = withEmbeddings + withoutEmbeddings;
        
        double coverage = total > 0 ? (double) withEmbeddings / total * 100 : 0;
        
        log.info("Daily embedding stats: {}/{} articles have embeddings ({:.1f}% coverage)", 
            withEmbeddings, total, coverage);
    }
}
