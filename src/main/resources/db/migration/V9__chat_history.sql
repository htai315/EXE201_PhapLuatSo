-- Chat history tables for legal chatbot

-- Chat sessions (conversations)
CREATE TABLE dbo.chat_sessions (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title NVARCHAR(200) NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_chat_session_user FOREIGN KEY (user_id) REFERENCES dbo.users(id) ON DELETE CASCADE
);

CREATE INDEX ix_chat_sessions_user_updated ON dbo.chat_sessions(user_id, updated_at DESC);

-- Chat messages
CREATE TABLE dbo.chat_messages (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    session_id BIGINT NOT NULL,
    role NVARCHAR(20) NOT NULL, -- USER or ASSISTANT
    content NVARCHAR(MAX) NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_chat_message_session FOREIGN KEY (session_id) REFERENCES dbo.chat_sessions(id) ON DELETE CASCADE,
    CONSTRAINT ck_chat_message_role CHECK (role IN ('USER', 'ASSISTANT'))
);

CREATE INDEX ix_chat_messages_session ON dbo.chat_messages(session_id, created_at ASC);

-- Chat message citations (many-to-many with legal_articles)
CREATE TABLE dbo.chat_message_citations (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    message_id BIGINT NOT NULL,
    article_id BIGINT NOT NULL,
    CONSTRAINT fk_citation_message FOREIGN KEY (message_id) REFERENCES dbo.chat_messages(id) ON DELETE CASCADE,
    CONSTRAINT fk_citation_article FOREIGN KEY (article_id) REFERENCES dbo.legal_articles(id)
);

CREATE INDEX ix_citations_message ON dbo.chat_message_citations(message_id);
CREATE INDEX ix_citations_article ON dbo.chat_message_citations(article_id);
