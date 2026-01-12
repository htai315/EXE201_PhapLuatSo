package com.htai.exe201phapluatso.legal.controller;

import com.htai.exe201phapluatso.legal.entity.LegalArticle;
import com.htai.exe201phapluatso.legal.repo.LegalArticleRepo;
import com.htai.exe201phapluatso.legal.repo.LegalDocumentRepo;
import com.htai.exe201phapluatso.legal.service.VectorSearchService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Profile("!production")
@RestController
@RequestMapping("/api/legal/debug")
public class LegalDebugController {

    private final LegalDocumentRepo documentRepo;
    private final LegalArticleRepo articleRepo;
    private final VectorSearchService vectorSearchService;

    public LegalDebugController(
            LegalDocumentRepo documentRepo, 
            LegalArticleRepo articleRepo,
            VectorSearchService vectorSearchService
    ) {
        this.documentRepo = documentRepo;
        this.articleRepo = articleRepo;
        this.vectorSearchService = vectorSearchService;
    }

    /**
     * Get statistics including embedding coverage
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalDocuments", documentRepo.count());
        stats.put("totalArticles", articleRepo.count());
        
        // Embedding stats
        long withEmbeddings = vectorSearchService.countArticlesWithEmbeddings();
        long withoutEmbeddings = vectorSearchService.countArticlesWithoutEmbeddings();
        stats.put("articlesWithEmbeddings", withEmbeddings);
        stats.put("articlesWithoutEmbeddings", withoutEmbeddings);
        stats.put("embeddingCoverage", withEmbeddings + withoutEmbeddings > 0 
            ? Math.round((double) withEmbeddings / (withEmbeddings + withoutEmbeddings) * 100) + "%" 
            : "N/A");
        
        return ResponseEntity.ok(stats);
    }

    /**
     * Test semantic search
     */
    @GetMapping("/search/semantic")
    public ResponseEntity<List<Map<String, Object>>> testSemanticSearch(
            @RequestParam String q,
            @RequestParam(defaultValue = "5") int limit
    ) {
        List<LegalArticle> results = vectorSearchService.semanticSearch(q, limit);
        
        List<Map<String, Object>> response = results.stream()
                .map(a -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", a.getId());
                    map.put("documentName", a.getDocument().getDocumentName());
                    map.put("articleNumber", a.getArticleNumber());
                    map.put("articleTitle", a.getArticleTitle());
                    map.put("contentPreview", a.getContent() != null && a.getContent().length() > 300 
                            ? a.getContent().substring(0, 300) + "..." 
                            : a.getContent());
                    return map;
                })
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Test hybrid search
     */
    @GetMapping("/search/hybrid")
    public ResponseEntity<List<Map<String, Object>>> testHybridSearch(
            @RequestParam String q,
            @RequestParam(defaultValue = "5") int limit
    ) {
        List<LegalArticle> results = vectorSearchService.hybridSearch(q, limit);
        
        List<Map<String, Object>> response = results.stream()
                .map(a -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", a.getId());
                    map.put("documentName", a.getDocument().getDocumentName());
                    map.put("articleNumber", a.getArticleNumber());
                    map.put("articleTitle", a.getArticleTitle());
                    map.put("contentPreview", a.getContent() != null && a.getContent().length() > 300 
                            ? a.getContent().substring(0, 300) + "..." 
                            : a.getContent());
                    return map;
                })
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get articles by document ID
     */
    @GetMapping("/articles/{documentId}")
    public ResponseEntity<List<Map<String, Object>>> getArticles(@PathVariable Long documentId) {
        List<LegalArticle> articles = articleRepo.findByDocumentIdOrderByArticleNumber(documentId);
        
        List<Map<String, Object>> result = articles.stream()
                .map(a -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", a.getId());
                    map.put("articleNumber", a.getArticleNumber());
                    map.put("articleTitle", a.getArticleTitle());
                    map.put("contentLength", a.getContent() != null ? a.getContent().length() : 0);
                    map.put("contentPreview", a.getContent() != null && a.getContent().length() > 200 
                            ? a.getContent().substring(0, 200) + "..." 
                            : a.getContent());
                    return map;
                })
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(result);
    }

    /**
     * Get full article content
     */
    @GetMapping("/article/{id}")
    public ResponseEntity<Map<String, Object>> getArticle(@PathVariable Long id) {
        return articleRepo.findById(id)
                .map(a -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", a.getId());
                    map.put("documentName", a.getDocument().getDocumentName());
                    map.put("articleNumber", a.getArticleNumber());
                    map.put("articleTitle", a.getArticleTitle());
                    map.put("content", a.getContent());
                    return ResponseEntity.ok(map);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
