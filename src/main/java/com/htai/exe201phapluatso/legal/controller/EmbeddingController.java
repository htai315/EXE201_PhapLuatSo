package com.htai.exe201phapluatso.legal.controller;

import com.htai.exe201phapluatso.legal.service.VectorSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin controller for managing article embeddings
 * Used to generate and monitor vector embeddings for semantic search
 */
@RestController
@RequestMapping("/api/admin/embeddings")
@PreAuthorize("hasRole('ADMIN')")
public class EmbeddingController {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingController.class);

    private final VectorSearchService vectorSearchService;

    public EmbeddingController(VectorSearchService vectorSearchService) {
        this.vectorSearchService = vectorSearchService;
    }

    /**
     * Get embedding statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        long withEmbeddings = vectorSearchService.countArticlesWithEmbeddings();
        long withoutEmbeddings = vectorSearchService.countArticlesWithoutEmbeddings();
        long total = withEmbeddings + withoutEmbeddings;
        
        double percentage = total > 0 ? (double) withEmbeddings / total * 100 : 0;

        return ResponseEntity.ok(Map.of(
            "totalArticles", total,
            "withEmbeddings", withEmbeddings,
            "withoutEmbeddings", withoutEmbeddings,
            "coveragePercent", Math.round(percentage * 100) / 100.0,
            "status", withoutEmbeddings == 0 ? "COMPLETE" : "INCOMPLETE"
        ));
    }

    /**
     * Generate embeddings for a single article
     */
    @PostMapping("/generate/{articleId}")
    public ResponseEntity<Map<String, Object>> generateForArticle(@PathVariable Long articleId) {
        log.info("Generating embedding for article {}", articleId);
        
        try {
            vectorSearchService.generateAndSaveEmbedding(articleId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Embedding generated for article " + articleId
            ));
        } catch (Exception e) {
            log.error("Failed to generate embedding for article {}: {}", articleId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Generate embeddings for articles without embeddings (batch)
     */
    @PostMapping("/generate-batch")
    public ResponseEntity<Map<String, Object>> generateBatch(
            @RequestParam(defaultValue = "10") int batchSize
    ) {
        log.info("Generating embeddings for batch of {} articles", batchSize);
        
        // Limit batch size to prevent timeout
        int actualBatchSize = Math.min(batchSize, 50);
        
        try {
            int generated = vectorSearchService.generateMissingEmbeddings(actualBatchSize);
            long remaining = vectorSearchService.countArticlesWithoutEmbeddings();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "generated", generated,
                "remaining", remaining,
                "message", String.format("Generated %d embeddings, %d remaining", generated, remaining)
            ));
        } catch (Exception e) {
            log.error("Batch embedding generation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Generate all missing embeddings (processes everything in batches)
     * Warning: This can take a long time for large datasets
     */
    @PostMapping("/generate-all")
    public ResponseEntity<Map<String, Object>> generateAll() {
        long missing = vectorSearchService.countArticlesWithoutEmbeddings();
        
        if (missing == 0) {
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "All articles already have embeddings"
            ));
        }

        log.info("Starting to generate embeddings for {} articles", missing);
        
        // Process in batches of 20 - NO LIMIT, gen hết sạch
        int totalGenerated = 0;
        int batchSize = 20;
        
        while (true) {
            int generated = vectorSearchService.generateMissingEmbeddings(batchSize);
            totalGenerated += generated;
            
            if (generated == 0) {
                break; // No more articles to process
            }
            
            log.info("Progress: generated {} embeddings so far...", totalGenerated);
        }

        long remaining = vectorSearchService.countArticlesWithoutEmbeddings();
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "totalGenerated", totalGenerated,
            "remaining", remaining,
            "message", String.format("Generated %d embeddings, %d remaining", totalGenerated, remaining)
        ));
    }
}
