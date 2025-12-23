package com.htai.exe201phapluatso.legal.entity;

import com.htai.exe201phapluatso.auth.entity.User;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "legal_documents")
public class LegalDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_name", nullable = false, length = 500)
    private String documentName;

    @Column(name = "document_code", length = 100)
    private String documentCode;

    @Column(name = "document_type", length = 100)
    private String documentType;

    @Column(name = "issuing_body", length = 200)
    private String issuingBody;

    @Column(name = "effective_date")
    private LocalDate effectiveDate;

    @Column(name = "file_path", length = 1000)
    private String filePath;

    @Column(name = "total_articles")
    private Integer totalArticles = 0;

    @Column(length = 50)
    private String status = "Còn hiệu lực";

    @ManyToOne
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LegalArticle> articles = new ArrayList<>();

    // Getters and Setters
    public Long getId() { return id; }

    public String getDocumentName() { return documentName; }
    public void setDocumentName(String documentName) { this.documentName = documentName; }

    public String getDocumentCode() { return documentCode; }
    public void setDocumentCode(String documentCode) { this.documentCode = documentCode; }

    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }

    public String getIssuingBody() { return issuingBody; }
    public void setIssuingBody(String issuingBody) { this.issuingBody = issuingBody; }

    public LocalDate getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public Integer getTotalArticles() { return totalArticles; }
    public void setTotalArticles(Integer totalArticles) { this.totalArticles = totalArticles; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<LegalArticle> getArticles() { return articles; }
    public void setArticles(List<LegalArticle> articles) { this.articles = articles; }
}
