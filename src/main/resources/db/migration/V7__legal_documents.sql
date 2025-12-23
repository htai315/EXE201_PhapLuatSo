-- Legal Documents System
-- Phase 1: Admin upload and parse legal documents

-- Table 1: Legal Documents (văn bản pháp luật)
CREATE TABLE legal_documents (
    id BIGINT PRIMARY KEY IDENTITY(1,1),
    document_name NVARCHAR(500) NOT NULL,
    document_code NVARCHAR(100),
    document_type NVARCHAR(100),
    issuing_body NVARCHAR(200),
    effective_date DATE,
    file_path NVARCHAR(1000),
    total_articles INT DEFAULT 0,
    status NVARCHAR(50) DEFAULT N'Còn hiệu lực',
    created_by_id BIGINT,
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    updated_at DATETIME2,
    CONSTRAINT FK_legal_documents_user FOREIGN KEY (created_by_id) REFERENCES users(id)
);

-- Table 2: Legal Articles (điều luật)
CREATE TABLE legal_articles (
    id BIGINT PRIMARY KEY IDENTITY(1,1),
    document_id BIGINT NOT NULL,
    article_number INT NOT NULL,
    article_title NVARCHAR(1000),
    content NVARCHAR(MAX) NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    CONSTRAINT FK_legal_articles_document FOREIGN KEY (document_id) REFERENCES legal_documents(id) ON DELETE CASCADE
);

-- Indexes for performance
CREATE INDEX IX_legal_documents_status ON legal_documents(status);
CREATE INDEX IX_legal_documents_created_at ON legal_documents(created_at DESC);
CREATE INDEX IX_legal_articles_document_id ON legal_articles(document_id);
CREATE INDEX IX_legal_articles_article_number ON legal_articles(article_number);
