-- =====================================================
-- V5: Auth Security Improvements
-- Add account lockout, token rotation, and security audit logging
-- =====================================================

-- 1. Add lockout fields to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS failed_login_attempts INT DEFAULT 0;
ALTER TABLE users ADD COLUMN IF NOT EXISTS locked_until TIMESTAMP;

-- 2. Add token rotation fields to refresh_tokens table
ALTER TABLE refresh_tokens ADD COLUMN IF NOT EXISTS used_at TIMESTAMP;
ALTER TABLE refresh_tokens ADD COLUMN IF NOT EXISTS replaced_by_token_id BIGINT;

-- Add foreign key for token chain tracking
ALTER TABLE refresh_tokens 
    ADD CONSTRAINT fk_refresh_token_replaced_by 
    FOREIGN KEY (replaced_by_token_id) 
    REFERENCES refresh_tokens(id) 
    ON DELETE SET NULL;

-- 3. Create security audit log table
CREATE TABLE IF NOT EXISTS security_audit_log (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    user_id BIGINT,
    email VARCHAR(255),
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    endpoint VARCHAR(255),
    details TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    CONSTRAINT fk_security_audit_user 
        FOREIGN KEY (user_id) 
        REFERENCES users(id) 
        ON DELETE SET NULL
);

-- 4. Create indexes for security audit log (optimized for common queries)
CREATE INDEX IF NOT EXISTS ix_security_audit_event_type ON security_audit_log(event_type);
CREATE INDEX IF NOT EXISTS ix_security_audit_user_id ON security_audit_log(user_id);
CREATE INDEX IF NOT EXISTS ix_security_audit_ip ON security_audit_log(ip_address);
CREATE INDEX IF NOT EXISTS ix_security_audit_created_at ON security_audit_log(created_at DESC);

-- 5. Create composite index for common query patterns
CREATE INDEX IF NOT EXISTS ix_security_audit_user_event ON security_audit_log(user_id, event_type, created_at DESC);

-- 6. Add index for lockout queries on users table
CREATE INDEX IF NOT EXISTS ix_users_locked_until ON users(locked_until) WHERE locked_until IS NOT NULL;

-- 7. Add comment for documentation
COMMENT ON TABLE security_audit_log IS 'Stores security-related events for audit and monitoring purposes';
COMMENT ON COLUMN users.failed_login_attempts IS 'Number of consecutive failed login attempts';
COMMENT ON COLUMN users.locked_until IS 'Account lockout expiration timestamp';
COMMENT ON COLUMN refresh_tokens.used_at IS 'Timestamp when token was used for rotation (for reuse detection)';
COMMENT ON COLUMN refresh_tokens.replaced_by_token_id IS 'ID of the new token that replaced this one';
