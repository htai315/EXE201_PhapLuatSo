package com.htai.exe201phapluatso.legal.controller;

import com.htai.exe201phapluatso.legal.entity.LegalArticle;
import com.htai.exe201phapluatso.legal.repo.LegalArticleRepo;
import com.htai.exe201phapluatso.legal.repo.LegalDocumentRepo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/legal/debug")
public class LegalDebugController {

    private final LegalDocumentRepo documentRepo;
    private final LegalArticleRepo articleRepo;

    public LegalDebugController(LegalDocumentRepo documentRepo, LegalArticleRepo articleRepo) {
        this.documentRepo = documentRepo;
        this.articleRepo = articleRepo;
    }

    /**
     * Get statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalDocuments", documentRepo.count());
        stats.put("totalArticles", articleRepo.count());
        
        return ResponseEntity.ok(stats);
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
