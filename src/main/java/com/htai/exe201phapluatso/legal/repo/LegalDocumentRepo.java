package com.htai.exe201phapluatso.legal.repo;

import com.htai.exe201phapluatso.legal.entity.LegalDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LegalDocumentRepo extends JpaRepository<LegalDocument, Long> {
    
    List<LegalDocument> findByStatusOrderByCreatedAtDesc(String status);
    
    List<LegalDocument> findAllByOrderByCreatedAtDesc();
    
    Page<LegalDocument> findByDocumentNameContainingIgnoreCaseOrDocumentCodeContainingIgnoreCase(
            String documentName, String documentCode, Pageable pageable);
}
