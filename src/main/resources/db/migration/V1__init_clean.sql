-- ============================================================================
-- CLEAN DATABASE INITIALIZATION - Credits Model
-- Based on existing schema + Credits system
-- ============================================================================

-- ============================================================================
-- CORE TABLES: Authentication & Users
-- ============================================================================

CREATE TABLE dbo.roles (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE dbo.users (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    email NVARCHAR(255) NOT NULL UNIQUE,
    password_hash NVARCHAR(255) NULL,
    full_name NVARCHAR(255) NULL,
    avatar_url VARCHAR(500) NULL,
    provider NVARCHAR(20) NOT NULL DEFAULT 'LOCAL',
    provider_id NVARCHAR(255) NULL,
    email_verified BIT NOT NULL DEFAULT 0,
    is_enabled BIT NOT NULL DEFAULT 1,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE UNIQUE INDEX ux_users_provider ON dbo.users(provider, provider_id)
    WHERE provider_id IS NOT NULL;

CREATE TABLE dbo.user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES dbo.users(id),
    CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES dbo.roles(id)
);

CREATE TABLE dbo.refresh_tokens (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash NVARCHAR(255) NOT NULL UNIQUE,
    expires_at DATETIME2 NOT NULL,
    revoked_at DATETIME2 NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_rt_user FOREIGN KEY (user_id) REFERENCES dbo.users(id)
);

-- ============================================================================
-- CREDITS SYSTEM: Plans & Credits (NEW)
-- ============================================================================

CREATE TABLE dbo.plans (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    code NVARCHAR(50) NOT NULL UNIQUE,
    name NVARCHAR(100) NOT NULL,
    price INT NOT NULL DEFAULT 0,
    chat_credits INT NOT NULL DEFAULT 0,
    quiz_gen_credits INT NOT NULL DEFAULT 0,
    duration_months INT NOT NULL DEFAULT 12,
    description NVARCHAR(500) NULL,
    is_active BIT NOT NULL DEFAULT 1,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE TABLE dbo.user_credits (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    chat_credits INT NOT NULL DEFAULT 0,
    quiz_gen_credits INT NOT NULL DEFAULT 0,
    expires_at DATETIME2 NULL,
    updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_credits_user FOREIGN KEY (user_id) REFERENCES dbo.users(id)
);

CREATE UNIQUE INDEX ux_credits_user ON dbo.user_credits(user_id);

CREATE TABLE dbo.credit_transactions (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type NVARCHAR(20) NOT NULL,
    credit_type NVARCHAR(20) NOT NULL,
    amount INT NOT NULL,
    balance_after INT NOT NULL,
    plan_code NVARCHAR(50) NULL,
    description NVARCHAR(500) NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_trans_user FOREIGN KEY (user_id) REFERENCES dbo.users(id),
    CONSTRAINT ck_trans_type CHECK (type IN ('PURCHASE', 'USAGE', 'BONUS', 'REFUND', 'EXPIRE')),
    CONSTRAINT ck_trans_credit_type CHECK (credit_type IN ('CHAT', 'QUIZ_GEN'))
);

CREATE INDEX ix_trans_user_date ON dbo.credit_transactions(user_id, created_at DESC);

-- ============================================================================
-- QUIZ SYSTEM (Keep existing structure)
-- ============================================================================

CREATE TABLE dbo.quiz_sets (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    created_by BIGINT NOT NULL,
    title NVARCHAR(200) NOT NULL,
    description NVARCHAR(1000) NULL,
    visibility NVARCHAR(20) NOT NULL DEFAULT 'PRIVATE',
    status NVARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NULL,
    CONSTRAINT fk_quiz_sets_user FOREIGN KEY (created_by) REFERENCES dbo.users(id)
);

CREATE INDEX ix_quiz_sets_created_by ON dbo.quiz_sets(created_by);

CREATE TABLE dbo.quiz_questions (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    quiz_set_id BIGINT NOT NULL,
    question_text NVARCHAR(2000) NOT NULL,
    explanation NVARCHAR(2000) NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NULL,
    CONSTRAINT fk_questions_set FOREIGN KEY (quiz_set_id) REFERENCES dbo.quiz_sets(id) ON DELETE CASCADE
);

CREATE INDEX ix_questions_set ON dbo.quiz_questions(quiz_set_id);

CREATE TABLE dbo.quiz_question_options (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    question_id BIGINT NOT NULL,
    option_key CHAR(1) NOT NULL,
    option_text NVARCHAR(1000) NOT NULL,
    is_correct BIT NOT NULL DEFAULT 0,
    CONSTRAINT fk_options_question FOREIGN KEY (question_id) REFERENCES dbo.quiz_questions(id) ON DELETE CASCADE,
    CONSTRAINT ck_option_key CHECK (option_key IN ('A','B','C','D'))
);

CREATE UNIQUE INDEX ux_options_question_key ON dbo.quiz_question_options(question_id, option_key);

CREATE TABLE dbo.quiz_attempts (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    quiz_set_id BIGINT NOT NULL,
    started_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    finished_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    total_questions INT NOT NULL,
    correct_count INT NOT NULL,
    score_percent INT NOT NULL,
    CONSTRAINT fk_quiz_attempt_user FOREIGN KEY (user_id) REFERENCES dbo.users(id),
    CONSTRAINT fk_quiz_attempt_set FOREIGN KEY (quiz_set_id) REFERENCES dbo.quiz_sets(id) ON DELETE CASCADE
);

CREATE TABLE dbo.quiz_attempt_answers (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    attempt_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    selected_option_key CHAR(1) NOT NULL,
    is_correct BIT NOT NULL,
    CONSTRAINT fk_quiz_attempt_answer_attempt FOREIGN KEY (attempt_id) REFERENCES dbo.quiz_attempts(id) ON DELETE CASCADE,
    CONSTRAINT fk_quiz_attempt_answer_question FOREIGN KEY (question_id) REFERENCES dbo.quiz_questions(id) ON DELETE NO ACTION
);

-- ============================================================================
-- LEGAL DOCUMENTS SYSTEM (Keep existing structure)
-- ============================================================================

CREATE TABLE dbo.legal_documents (
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
    CONSTRAINT FK_legal_documents_user FOREIGN KEY (created_by_id) REFERENCES dbo.users(id)
);

CREATE INDEX IX_legal_documents_status ON dbo.legal_documents(status);
CREATE INDEX IX_legal_documents_created_at ON dbo.legal_documents(created_at DESC);

CREATE TABLE dbo.legal_articles (
    id BIGINT PRIMARY KEY IDENTITY(1,1),
    document_id BIGINT NOT NULL,
    article_number INT NOT NULL,
    article_title NVARCHAR(1000),
    content NVARCHAR(MAX) NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    CONSTRAINT FK_legal_articles_document FOREIGN KEY (document_id) REFERENCES dbo.legal_documents(id) ON DELETE CASCADE
);

CREATE INDEX IX_legal_articles_document_id ON dbo.legal_articles(document_id);
CREATE INDEX IX_legal_articles_article_number ON dbo.legal_articles(article_number);

-- ============================================================================
-- CHAT HISTORY SYSTEM (Keep existing structure)
-- ============================================================================

CREATE TABLE dbo.chat_sessions (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title NVARCHAR(200) NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_chat_session_user FOREIGN KEY (user_id) REFERENCES dbo.users(id) ON DELETE CASCADE
);

CREATE INDEX ix_chat_sessions_user_updated ON dbo.chat_sessions(user_id, updated_at DESC);

CREATE TABLE dbo.chat_messages (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    session_id BIGINT NOT NULL,
    role NVARCHAR(20) NOT NULL,
    content NVARCHAR(MAX) NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_chat_message_session FOREIGN KEY (session_id) REFERENCES dbo.chat_sessions(id) ON DELETE CASCADE,
    CONSTRAINT ck_chat_message_role CHECK (role IN ('USER', 'ASSISTANT'))
);

CREATE INDEX ix_chat_messages_session ON dbo.chat_messages(session_id, created_at ASC);

CREATE TABLE dbo.chat_message_citations (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    message_id BIGINT NOT NULL,
    article_id BIGINT NOT NULL,
    CONSTRAINT fk_citation_message FOREIGN KEY (message_id) REFERENCES dbo.chat_messages(id) ON DELETE CASCADE,
    CONSTRAINT fk_citation_article FOREIGN KEY (article_id) REFERENCES dbo.legal_articles(id)
);

CREATE INDEX ix_citations_message ON dbo.chat_message_citations(message_id);
CREATE INDEX ix_citations_article ON dbo.chat_message_citations(article_id);

-- ============================================================================
-- SEED DATA
-- ============================================================================

-- Roles
INSERT INTO dbo.roles(name) VALUES (N'USER'), (N'ADMIN');

-- Plans with credits
INSERT INTO dbo.plans(code, name, price, chat_credits, quiz_gen_credits, duration_months, description) 
VALUES
    (N'FREE', N'Miễn Phí', 0, 10, 0, 0, N'Gói dùng thử với 10 lượt chat AI'),
    (N'REGULAR', N'Người Dân', 159000, 100, 0, 12, N'100 lượt chat AI, hạn sử dụng 12 tháng'),
    (N'STUDENT', N'Sinh Viên', 249000, 100, 20, 12, N'100 lượt chat AI + 20 lượt AI tạo đề, hạn sử dụng 12 tháng');

-- ============================================================================
-- TRIGGERS
-- ============================================================================

-- Trigger: Only one correct option per question
GO
CREATE TRIGGER dbo.trg_only_one_correct_option
ON dbo.quiz_question_options
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    
    IF EXISTS (
        SELECT 1
        FROM inserted i
        JOIN dbo.quiz_question_options o ON o.question_id = i.question_id
        GROUP BY o.question_id
        HAVING SUM(CASE WHEN o.is_correct = 1 THEN 1 ELSE 0 END) > 1
    )
    BEGIN
        RAISERROR(N'Each question can have only one correct option.', 16, 1);
        ROLLBACK TRANSACTION;
    END
END;
GO

-- Trigger: Auto give FREE credits to new users
GO
CREATE TRIGGER trg_users_give_free_credits
ON dbo.users
AFTER INSERT
AS
BEGIN
    SET NOCOUNT ON;
    
    INSERT INTO dbo.user_credits(user_id, chat_credits, quiz_gen_credits, expires_at)
    SELECT id, 10, 0, NULL
    FROM inserted;
    
    INSERT INTO dbo.credit_transactions(user_id, type, credit_type, amount, balance_after, description)
    SELECT id, 'BONUS', 'CHAT', 10, 10, N'Welcome bonus - 10 free chat credits'
    FROM inserted;
END;
GO

-- ============================================================================
-- END OF MIGRATION
-- ============================================================================
