-- ============================================================================
-- POSTGRESQL DATABASE INITIALIZATION - CLEAN VERSION
-- Consolidated from V1-V10 migrations
-- Last updated: January 2026
-- ============================================================================

-- Enable pgvector extension for vector search
CREATE EXTENSION IF NOT EXISTS vector;

-- ============================================================================
-- CORE TABLES: Authentication & Users
-- ============================================================================

CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NULL,
    full_name VARCHAR(255) NULL,
    avatar_url VARCHAR(500) NULL,
    provider VARCHAR(20) NOT NULL DEFAULT 'LOCAL',
    provider_id VARCHAR(255) NULL,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    ban_reason VARCHAR(500) NULL,
    banned_at TIMESTAMP NULL,
    banned_by BIGINT NULL,
    -- V5: Auth security fields
    failed_login_attempts INT DEFAULT 0,
    locked_until TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX ux_users_provider ON users(provider, provider_id) WHERE provider_id IS NOT NULL;
CREATE INDEX ix_users_is_active ON users(is_active);
CREATE INDEX ix_users_created_at ON users(created_at DESC);
CREATE INDEX ix_users_locked_until ON users(locked_until) WHERE locked_until IS NOT NULL;

ALTER TABLE users ADD CONSTRAINT fk_users_banned_by 
    FOREIGN KEY (banned_by) REFERENCES users(id);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES roles(id)
);

CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP NULL,
    -- V5: Token rotation fields
    used_at TIMESTAMP NULL,
    replaced_by_token_id BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_rt_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_refresh_token_replaced_by FOREIGN KEY (replaced_by_token_id) REFERENCES refresh_tokens(id) ON DELETE SET NULL
);


-- ============================================================================
-- CREDITS SYSTEM: Plans & Credits
-- ============================================================================

CREATE TABLE plans (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    price INT NOT NULL DEFAULT 0,
    chat_credits INT NOT NULL DEFAULT 0,
    quiz_gen_credits INT NOT NULL DEFAULT 0,
    duration_months INT NOT NULL DEFAULT 12,
    description VARCHAR(500) NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE user_credits (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    chat_credits INT NOT NULL DEFAULT 0,
    quiz_gen_credits INT NOT NULL DEFAULT 0,
    expires_at TIMESTAMP NULL,
    -- V7: Optimistic locking
    version INTEGER NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_credits_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE UNIQUE INDEX ux_credits_user ON user_credits(user_id);

CREATE TABLE credit_transactions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    credit_type VARCHAR(20) NOT NULL,
    amount INT NOT NULL,
    balance_after INT NOT NULL,
    plan_code VARCHAR(50) NULL,
    description VARCHAR(500) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_trans_user FOREIGN KEY (user_id) REFERENCES users(id),
    -- V8/V9: Extended types for admin and reservation
    CONSTRAINT ck_trans_type CHECK (type IN ('PURCHASE', 'USAGE', 'BONUS', 'REFUND', 'EXPIRE', 'ADMIN_ADD', 'ADMIN_REMOVE', 'RESERVE', 'CONFIRM')),
    CONSTRAINT ck_trans_credit_type CHECK (credit_type IN ('CHAT', 'QUIZ_GEN'))
);

CREATE INDEX ix_trans_user_date ON credit_transactions(user_id, created_at DESC);
CREATE INDEX ix_credit_trans_date ON credit_transactions(created_at DESC);

-- V7: Credit reservations table
CREATE TABLE credit_reservations (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    credit_type VARCHAR(20) NOT NULL,
    amount INTEGER NOT NULL DEFAULT 1,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    confirmed_at TIMESTAMP,
    refunded_at TIMESTAMP,
    operation_type VARCHAR(50),
    CONSTRAINT chk_reservation_status CHECK (status IN ('PENDING', 'CONFIRMED', 'REFUNDED', 'EXPIRED')),
    CONSTRAINT chk_reservation_amount CHECK (amount > 0)
);

CREATE INDEX idx_credit_reservations_user_status ON credit_reservations(user_id, status);
CREATE INDEX idx_credit_reservations_expires ON credit_reservations(expires_at) WHERE status = 'PENDING';

-- ============================================================================
-- QUIZ SYSTEM
-- ============================================================================

CREATE TABLE quiz_sets (
    id BIGSERIAL PRIMARY KEY,
    created_by BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(1000) NULL,
    visibility VARCHAR(20) NOT NULL DEFAULT 'PRIVATE',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    -- V3: Quiz duration
    duration_minutes INTEGER NOT NULL DEFAULT 45,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NULL,
    CONSTRAINT fk_quiz_sets_user FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE INDEX ix_quiz_sets_created_by ON quiz_sets(created_by);
CREATE INDEX ix_quiz_sets_created_at ON quiz_sets(created_at DESC);

CREATE TABLE quiz_questions (
    id BIGSERIAL PRIMARY KEY,
    quiz_set_id BIGINT NOT NULL,
    question_text VARCHAR(2000) NOT NULL,
    explanation VARCHAR(2000) NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NULL,
    CONSTRAINT fk_questions_set FOREIGN KEY (quiz_set_id) REFERENCES quiz_sets(id) ON DELETE CASCADE
);

CREATE INDEX ix_questions_set ON quiz_questions(quiz_set_id);

CREATE TABLE quiz_question_options (
    id BIGSERIAL PRIMARY KEY,
    question_id BIGINT NOT NULL,
    option_key VARCHAR(1) NOT NULL,
    option_text VARCHAR(1000) NOT NULL,
    is_correct BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_options_question FOREIGN KEY (question_id) REFERENCES quiz_questions(id) ON DELETE CASCADE,
    CONSTRAINT ck_option_key CHECK (option_key IN ('A','B','C','D'))
);

CREATE UNIQUE INDEX ux_options_question_key ON quiz_question_options(question_id, option_key);

CREATE TABLE quiz_attempts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    quiz_set_id BIGINT NOT NULL,
    started_at TIMESTAMP NOT NULL DEFAULT NOW(),
    finished_at TIMESTAMP NOT NULL DEFAULT NOW(),
    total_questions INT NOT NULL,
    correct_count INT NOT NULL,
    score_percent INT NOT NULL,
    CONSTRAINT fk_quiz_attempt_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_quiz_attempt_set FOREIGN KEY (quiz_set_id) REFERENCES quiz_sets(id) ON DELETE CASCADE
);

CREATE TABLE quiz_attempt_answers (
    id BIGSERIAL PRIMARY KEY,
    attempt_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    selected_option_key VARCHAR(1) NOT NULL,
    is_correct BOOLEAN NOT NULL,
    CONSTRAINT fk_quiz_attempt_answer_attempt FOREIGN KEY (attempt_id) REFERENCES quiz_attempts(id) ON DELETE CASCADE,
    CONSTRAINT fk_quiz_attempt_answer_question FOREIGN KEY (question_id) REFERENCES quiz_questions(id) ON DELETE NO ACTION
);


-- ============================================================================
-- LEGAL DOCUMENTS SYSTEM
-- ============================================================================

CREATE TABLE legal_documents (
    id BIGSERIAL PRIMARY KEY,
    document_name VARCHAR(500) NOT NULL,
    document_code VARCHAR(100),
    document_type VARCHAR(100),
    issuing_body VARCHAR(200),
    effective_date DATE,
    file_path VARCHAR(1000),
    total_articles INT DEFAULT 0,
    status VARCHAR(50) DEFAULT 'Còn hiệu lực',
    created_by_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    CONSTRAINT fk_legal_documents_user FOREIGN KEY (created_by_id) REFERENCES users(id)
);

CREATE INDEX ix_legal_documents_status ON legal_documents(status);
CREATE INDEX ix_legal_documents_created_at ON legal_documents(created_at DESC);

CREATE TABLE legal_articles (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL,
    article_number INT NOT NULL,
    article_title VARCHAR(1000),
    content TEXT NOT NULL,
    -- V2: Vector search columns
    embedding vector(1536) NULL,
    embedding_updated_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_legal_articles_document FOREIGN KEY (document_id) REFERENCES legal_documents(id) ON DELETE CASCADE
);

CREATE INDEX ix_legal_articles_document_id ON legal_articles(document_id);
CREATE INDEX ix_legal_articles_article_number ON legal_articles(article_number);
-- V2: Vector search index
CREATE INDEX ix_legal_articles_embedding ON legal_articles USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

-- ============================================================================
-- CHAT HISTORY SYSTEM
-- ============================================================================

CREATE TABLE chat_sessions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_chat_session_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX ix_chat_sessions_user_updated ON chat_sessions(user_id, updated_at DESC);
CREATE INDEX ix_chat_sessions_created_at ON chat_sessions(created_at DESC);

CREATE TABLE chat_messages (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_chat_message_session FOREIGN KEY (session_id) REFERENCES chat_sessions(id) ON DELETE CASCADE,
    CONSTRAINT ck_chat_message_role CHECK (role IN ('USER', 'ASSISTANT'))
);

CREATE INDEX ix_chat_messages_session ON chat_messages(session_id, created_at ASC);

CREATE TABLE chat_message_citations (
    id BIGSERIAL PRIMARY KEY,
    message_id BIGINT NOT NULL,
    article_id BIGINT NOT NULL,
    CONSTRAINT fk_citation_message FOREIGN KEY (message_id) REFERENCES chat_messages(id) ON DELETE CASCADE,
    CONSTRAINT fk_citation_article FOREIGN KEY (article_id) REFERENCES legal_articles(id)
);

CREATE INDEX ix_citations_message ON chat_message_citations(message_id);
CREATE INDEX ix_citations_article ON chat_message_citations(article_id);

-- ============================================================================
-- PAYMENT SYSTEM
-- ============================================================================

CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    plan_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    
    -- VNPay fields
    vnp_txn_ref VARCHAR(100) UNIQUE NOT NULL,
    vnp_transaction_no VARCHAR(100),
    vnp_bank_code VARCHAR(20),
    vnp_card_type VARCHAR(20),
    
    -- PayOS fields
    order_code BIGINT NULL,
    transaction_id VARCHAR(100) NULL,
    webhook_processed BOOLEAN DEFAULT FALSE,
    -- V4: QR code fields
    checkout_url VARCHAR(500),
    qr_code TEXT,
    
    -- Status
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payment_method VARCHAR(20) DEFAULT 'VNPAY',
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    paid_at TIMESTAMP,
    
    -- Metadata
    ip_address VARCHAR(50),
    
    CONSTRAINT fk_payments_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_payments_plan FOREIGN KEY (plan_id) REFERENCES plans(id)
);

CREATE INDEX ix_payments_user_id ON payments(user_id);
CREATE INDEX ix_payments_plan_id ON payments(plan_id);
CREATE INDEX ix_payments_vnp_txn_ref ON payments(vnp_txn_ref);
CREATE INDEX ix_payments_status ON payments(status);
CREATE INDEX ix_payments_status_date ON payments(status, created_at DESC);
CREATE UNIQUE INDEX ix_payments_order_code ON payments(order_code) WHERE order_code IS NOT NULL;

-- V6: Payment idempotency records
CREATE TABLE payment_idempotency_records (
    id BIGSERIAL PRIMARY KEY,
    scoped_key VARCHAR(255) NOT NULL,
    user_id BIGINT NOT NULL,
    plan_code VARCHAR(50),
    payment_id BIGINT,
    status VARCHAR(20) DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_idempotency_scoped_key UNIQUE (scoped_key),
    CONSTRAINT fk_idempotency_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_idempotency_payment FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE SET NULL
);

CREATE INDEX idx_idempotency_expires_at ON payment_idempotency_records(expires_at);
CREATE INDEX idx_idempotency_user_id ON payment_idempotency_records(user_id);


-- ============================================================================
-- PASSWORD RESET OTP
-- ============================================================================

CREATE TABLE password_reset_otps (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    otp VARCHAR(6) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    is_used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX ix_password_reset_email ON password_reset_otps(email);
CREATE INDEX ix_password_reset_otp ON password_reset_otps(otp);
CREATE INDEX ix_password_reset_expires ON password_reset_otps(expires_at);

-- ============================================================================
-- EMAIL VERIFICATION TOKENS
-- ============================================================================

CREATE TABLE email_verification_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    verified_at TIMESTAMP,
    CONSTRAINT fk_email_verification_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX ix_email_verification_token ON email_verification_tokens(token);
CREATE INDEX ix_email_verification_user ON email_verification_tokens(user_id);
CREATE INDEX ix_email_verification_expires ON email_verification_tokens(expires_at);

-- ============================================================================
-- ADMIN ACTIVITY LOG
-- ============================================================================

CREATE TABLE admin_activity_logs (
    id BIGSERIAL PRIMARY KEY,
    admin_user_id BIGINT NOT NULL,
    action_type VARCHAR(50) NOT NULL,
    target_type VARCHAR(50) NOT NULL,
    target_id BIGINT NULL,
    description VARCHAR(1000) NULL,
    ip_address VARCHAR(50) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_admin_logs_user FOREIGN KEY (admin_user_id) REFERENCES users(id),
    -- V8: Extended action types
    CONSTRAINT ck_action_type CHECK (action_type IN ('BAN_USER', 'UNBAN_USER', 'DELETE_USER', 'VIEW_USER', 'DELETE_PAYMENT', 'DELETE_DOCUMENT', 'ADD_CREDITS', 'REMOVE_CREDITS', 'OTHER'))
);

CREATE INDEX ix_admin_logs_admin_user ON admin_activity_logs(admin_user_id, created_at DESC);
CREATE INDEX ix_admin_logs_created_at ON admin_activity_logs(created_at DESC);

-- V5: Security audit log
CREATE TABLE security_audit_log (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    user_id BIGINT,
    email VARCHAR(255),
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    endpoint VARCHAR(255),
    details TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_security_audit_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX ix_security_audit_event_type ON security_audit_log(event_type);
CREATE INDEX ix_security_audit_user_id ON security_audit_log(user_id);
CREATE INDEX ix_security_audit_ip ON security_audit_log(ip_address);
CREATE INDEX ix_security_audit_created_at ON security_audit_log(created_at DESC);
CREATE INDEX ix_security_audit_user_event ON security_audit_log(user_id, event_type, created_at DESC);

-- ============================================================================
-- SEQUENCE FOR ORDER CODE
-- ============================================================================

CREATE SEQUENCE order_code_sequence
    START WITH 77777777
    INCREMENT BY 1
    MINVALUE 77777777
    MAXVALUE 99999999
    NO CYCLE;

-- ============================================================================
-- SEED DATA
-- ============================================================================

-- Roles
INSERT INTO roles(name) VALUES ('USER'), ('ADMIN');

-- Plans with NEW PRICES (V10)
INSERT INTO plans(code, name, price, chat_credits, quiz_gen_credits, duration_months, description) 
VALUES
    ('FREE', 'Miễn Phí', 0, 10, 0, 0, 'Gói dùng thử với 10 lượt chat AI'),
    ('REGULAR', 'Người Dân', 59000, 100, 0, 12, '100 lượt chat AI, hạn sử dụng 12 tháng'),
    ('STUDENT', 'Sinh Viên', 99000, 100, 20, 12, '100 lượt chat AI + 20 lượt AI tạo đề, hạn sử dụng 12 tháng');


-- ============================================================================
-- TRIGGERS
-- ============================================================================

-- Trigger: Only one correct option per question
CREATE OR REPLACE FUNCTION check_only_one_correct_option()
RETURNS TRIGGER AS $$
BEGIN
    IF (SELECT COUNT(*) FROM quiz_question_options 
        WHERE question_id = NEW.question_id AND is_correct = TRUE) > 1 THEN
        RAISE EXCEPTION 'Each question can have only one correct option.';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_only_one_correct_option
AFTER INSERT OR UPDATE ON quiz_question_options
FOR EACH ROW
WHEN (NEW.is_correct = TRUE)
EXECUTE FUNCTION check_only_one_correct_option();

-- Trigger: Auto give FREE credits to new users
CREATE OR REPLACE FUNCTION give_free_credits_to_new_user()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO user_credits(user_id, chat_credits, quiz_gen_credits, expires_at, version)
    VALUES (NEW.id, 10, 0, NULL, 0);
    
    INSERT INTO credit_transactions(user_id, type, credit_type, amount, balance_after, description)
    VALUES (NEW.id, 'BONUS', 'CHAT', 10, 10, 'Welcome bonus - 10 free chat credits');
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_users_give_free_credits
AFTER INSERT ON users
FOR EACH ROW
EXECUTE FUNCTION give_free_credits_to_new_user();

-- ============================================================================
-- V2: VECTOR SEARCH FUNCTIONS
-- ============================================================================

-- Function to search by vector similarity
CREATE OR REPLACE FUNCTION search_articles_by_vector(
    query_embedding vector(1536),
    similarity_threshold FLOAT DEFAULT 0.3,
    max_results INT DEFAULT 10
)
RETURNS TABLE (
    article_id BIGINT,
    document_id BIGINT,
    article_number INT,
    article_title VARCHAR(1000),
    content TEXT,
    document_name VARCHAR(500),
    document_status VARCHAR(50),
    similarity FLOAT
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        a.id AS article_id,
        a.document_id,
        a.article_number,
        a.article_title,
        a.content,
        d.document_name,
        d.status AS document_status,
        1 - (a.embedding <=> query_embedding) AS similarity
    FROM legal_articles a
    JOIN legal_documents d ON a.document_id = d.id
    WHERE a.embedding IS NOT NULL
      AND d.status = 'Còn hiệu lực'
      AND 1 - (a.embedding <=> query_embedding) >= similarity_threshold
    ORDER BY a.embedding <=> query_embedding
    LIMIT max_results;
END;
$$ LANGUAGE plpgsql;

-- Hybrid search function (vector + keyword)
CREATE OR REPLACE FUNCTION hybrid_search_articles(
    query_embedding vector(1536),
    keywords TEXT[],
    vector_weight FLOAT DEFAULT 0.7,
    keyword_weight FLOAT DEFAULT 0.3,
    max_results INT DEFAULT 10
)
RETURNS TABLE (
    article_id BIGINT,
    document_id BIGINT,
    article_number INT,
    article_title VARCHAR(1000),
    content TEXT,
    document_name VARCHAR(500),
    vector_score FLOAT,
    keyword_score FLOAT,
    combined_score FLOAT
) AS $$
BEGIN
    RETURN QUERY
    WITH vector_results AS (
        SELECT 
            a.id,
            a.document_id,
            a.article_number,
            a.article_title,
            a.content,
            d.document_name,
            CASE 
                WHEN a.embedding IS NOT NULL 
                THEN 1 - (a.embedding <=> query_embedding)
                ELSE 0 
            END AS v_score
        FROM legal_articles a
        JOIN legal_documents d ON a.document_id = d.id
        WHERE d.status = 'Còn hiệu lực'
    ),
    keyword_results AS (
        SELECT 
            vr.id,
            COALESCE(
                (SELECT SUM(
                    CASE 
                        WHEN vr.article_title ILIKE '%' || kw || '%' THEN 3
                        ELSE 0 
                    END +
                    CASE 
                        WHEN vr.content ILIKE '%' || kw || '%' THEN 1
                        ELSE 0 
                    END
                ) FROM unnest(keywords) AS kw),
                0
            )::FLOAT / GREATEST(array_length(keywords, 1), 1) AS k_score
        FROM vector_results vr
    )
    SELECT 
        vr.id AS article_id,
        vr.document_id,
        vr.article_number,
        vr.article_title,
        vr.content,
        vr.document_name,
        vr.v_score AS vector_score,
        kr.k_score AS keyword_score,
        (vr.v_score * vector_weight + kr.k_score * keyword_weight) AS combined_score
    FROM vector_results vr
    JOIN keyword_results kr ON vr.id = kr.id
    WHERE vr.v_score > 0.2 OR kr.k_score > 0
    ORDER BY (vr.v_score * vector_weight + kr.k_score * keyword_weight) DESC
    LIMIT max_results;
END;
$$ LANGUAGE plpgsql;


-- ============================================================================
-- ADMIN VIEW FOR STATISTICS
-- ============================================================================

CREATE OR REPLACE VIEW vw_admin_dashboard_stats AS
SELECT
    (SELECT COUNT(*) FROM users) AS total_users,
    (SELECT COUNT(*) FROM users WHERE is_active = TRUE) AS active_users,
    (SELECT COUNT(*) FROM users WHERE is_active = FALSE) AS banned_users,
    (SELECT COUNT(*) FROM users WHERE created_at >= NOW() - INTERVAL '30 days') AS new_users_last_30_days,
    (SELECT COUNT(*) FROM payments WHERE status = 'SUCCESS') AS total_successful_payments,
    (SELECT COALESCE(SUM(amount), 0) FROM payments WHERE status = 'SUCCESS') AS total_revenue,
    (SELECT COALESCE(SUM(amount), 0) FROM payments WHERE status = 'SUCCESS' AND created_at >= NOW() - INTERVAL '30 days') AS revenue_last_30_days,
    (SELECT COUNT(*) FROM quiz_sets) AS total_quiz_sets,
    (SELECT COUNT(*) FROM quiz_attempts) AS total_quiz_attempts,
    (SELECT COUNT(*) FROM chat_sessions) AS total_chat_sessions,
    (SELECT COUNT(*) FROM chat_messages WHERE role = 'USER') AS total_chat_messages,
    (SELECT COUNT(*) FROM legal_documents) AS total_legal_documents,
    (SELECT COUNT(*) FROM legal_articles) AS total_legal_articles;

-- ============================================================================
-- COMMENTS FOR DOCUMENTATION
-- ============================================================================

COMMENT ON COLUMN users.is_active IS 'Indicates if user account is active (true) or banned (false)';
COMMENT ON COLUMN users.failed_login_attempts IS 'Number of consecutive failed login attempts';
COMMENT ON COLUMN users.locked_until IS 'Account lockout expiration timestamp';
COMMENT ON COLUMN refresh_tokens.used_at IS 'Timestamp when token was used for rotation';
COMMENT ON COLUMN refresh_tokens.replaced_by_token_id IS 'ID of the new token that replaced this one';
COMMENT ON COLUMN legal_articles.embedding IS 'Vector embedding from OpenAI text-embedding-3-small (1536 dimensions)';
COMMENT ON COLUMN legal_articles.embedding_updated_at IS 'Timestamp when embedding was last generated/updated';
COMMENT ON COLUMN quiz_sets.duration_minutes IS 'Exam duration in minutes (5-180, default 45)';
COMMENT ON COLUMN payments.checkout_url IS 'PayOS checkout URL for payment';
COMMENT ON COLUMN payments.qr_code IS 'VietQR string or base64 QR image for reuse';
COMMENT ON TABLE credit_reservations IS 'Tracks credit reservations for refund support when AI operations fail';
COMMENT ON TABLE payment_idempotency_records IS 'Lưu idempotency keys để tránh duplicate payment khi network retry';
COMMENT ON TABLE security_audit_log IS 'Stores security-related events for audit and monitoring purposes';
COMMENT ON SEQUENCE order_code_sequence IS 'Sequence for generating unique payment order codes. Range: 77777777-99999999';

-- ============================================================================
-- END OF CLEAN MIGRATION
-- ============================================================================
