package com.htai.exe201phapluatso.legal.repo;

import com.htai.exe201phapluatso.legal.entity.LegalArticle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LegalArticleRepo extends JpaRepository<LegalArticle, Long> {
    
    List<LegalArticle> findByDocumentIdOrderByArticleNumber(Long documentId);
    
    long countByDocumentId(Long documentId);
}
