package com.htai.exe201phapluatso.legal.repo;

import com.htai.exe201phapluatso.legal.entity.LegalArticle;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LegalArticleRepo extends JpaRepository<LegalArticle, Long> {
    
    List<LegalArticle> findByDocumentIdOrderByArticleNumber(Long documentId);
    
    long countByDocumentId(Long documentId);
    
    /**
     * Get recent articles with limit (for fallback search)
     */
    @Query("SELECT a FROM LegalArticle a JOIN a.document d WHERE d.status = 'Còn hiệu lực' ORDER BY a.id DESC")
    List<LegalArticle> findRecentArticles(Pageable pageable);
}
