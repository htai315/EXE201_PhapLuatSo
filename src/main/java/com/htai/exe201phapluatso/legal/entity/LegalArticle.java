package com.htai.exe201phapluatso.legal.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "legal_articles")
public class LegalArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "document_id")
    private LegalDocument document;

    @Column(name = "article_number", nullable = false)
    private Integer articleNumber;

    @Column(name = "article_title", length = 1000)
    private String articleTitle;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Getters and Setters
    public Long getId() { return id; }

    public LegalDocument getDocument() { return document; }
    public void setDocument(LegalDocument document) { this.document = document; }

    public Integer getArticleNumber() { return articleNumber; }
    public void setArticleNumber(Integer articleNumber) { this.articleNumber = articleNumber; }

    public String getArticleTitle() { return articleTitle; }
    public void setArticleTitle(String articleTitle) { this.articleTitle = articleTitle; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
