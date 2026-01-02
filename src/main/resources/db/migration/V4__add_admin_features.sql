-- ============================================================================
-- ADMIN DASHBOARD FEATURES
-- Add fields and indexes for admin management
-- ============================================================================

-- ============================================================================
-- 1. ADD ADMIN FIELDS TO USERS TABLE
-- ============================================================================

-- Add is_active field for ban/unban functionality
ALTER TABLE dbo.users 
ADD is_active BIT NOT NULL DEFAULT 1;

-- Add ban_reason field to store reason for banning
ALTER TABLE dbo.users 
ADD ban_reason NVARCHAR(500) NULL;

-- Add banned_at timestamp
ALTER TABLE dbo.users 
ADD banned_at DATETIME2 NULL;

-- Add banned_by field to track who banned the user
ALTER TABLE dbo.users 
ADD banned_by BIGINT NULL;

-- Foreign key for banned_by
ALTER TABLE dbo.users
ADD CONSTRAINT fk_users_banned_by FOREIGN KEY (banned_by) REFERENCES dbo.users(id);

-- ============================================================================
-- 2. ADD INDEXES FOR ADMIN QUERIES PERFORMANCE
-- ============================================================================

-- Index for filtering users by active status
CREATE INDEX ix_users_is_active ON dbo.users(is_active);

-- Index for filtering users by created date
CREATE INDEX ix_users_created_at ON dbo.users(created_at DESC);

-- Index for payments by status and date (for admin payment management)
IF EXISTS (SELECT * FROM sys.tables WHERE name = 'payments')
BEGIN
    CREATE INDEX ix_payments_status_date ON dbo.payments(status, created_at DESC);
END;

-- Index for credit transactions by date (for admin statistics)
CREATE INDEX ix_credit_trans_date ON dbo.credit_transactions(created_at DESC);

-- Index for quiz sets by created date (for admin statistics)
CREATE INDEX ix_quiz_sets_created_at ON dbo.quiz_sets(created_at DESC);

-- Index for chat sessions by created date (for admin statistics)
CREATE INDEX ix_chat_sessions_created_at ON dbo.chat_sessions(created_at DESC);

-- ============================================================================
-- 3. CREATE ADMIN ACTIVITY LOG TABLE (for audit trail)
-- ============================================================================

CREATE TABLE dbo.admin_activity_logs (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    admin_user_id BIGINT NOT NULL,
    action_type NVARCHAR(50) NOT NULL,
    target_type NVARCHAR(50) NOT NULL,
    target_id BIGINT NULL,
    description NVARCHAR(1000) NULL,
    ip_address NVARCHAR(50) NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_admin_logs_user FOREIGN KEY (admin_user_id) REFERENCES dbo.users(id),
    CONSTRAINT ck_action_type CHECK (action_type IN ('BAN_USER', 'UNBAN_USER', 'DELETE_USER', 'VIEW_USER', 'DELETE_PAYMENT', 'DELETE_DOCUMENT', 'OTHER'))
);

CREATE INDEX ix_admin_logs_admin_user ON dbo.admin_activity_logs(admin_user_id, created_at DESC);
CREATE INDEX ix_admin_logs_created_at ON dbo.admin_activity_logs(created_at DESC);

-- ============================================================================
-- 4. ADD ADMIN USER (if not exists)
-- ============================================================================

-- Create admin user if not exists
IF NOT EXISTS (SELECT 1 FROM dbo.users WHERE email = 'admin@phapluatso.vn')
BEGIN
    -- Insert admin user
    -- Password: Admin@123 (you should change this after first login!)
    -- Password hash will be set manually after migration
    INSERT INTO dbo.users (email, password_hash, full_name, provider, email_verified, is_enabled, created_at)
    VALUES (
        'admin@phapluatso.vn',
        '$2a$10$placeholder',  -- Placeholder, will be set manually
        N'System Administrator',
        'LOCAL',
        1,
        1,
        SYSUTCDATETIME()
    );

    -- Get admin user id
    DECLARE @adminUserId BIGINT;
    SELECT @adminUserId = id FROM dbo.users WHERE email = 'admin@phapluatso.vn';

    -- Assign ADMIN role
    DECLARE @adminRoleId BIGINT;
    SELECT @adminRoleId = id FROM dbo.roles WHERE name = 'ADMIN';

    IF @adminRoleId IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.user_roles WHERE user_id = @adminUserId AND role_id = @adminRoleId)
    BEGIN
        INSERT INTO dbo.user_roles (user_id, role_id)
        VALUES (@adminUserId, @adminRoleId);
    END;

    -- Give admin unlimited credits (only if not exists)
    IF NOT EXISTS (SELECT 1 FROM dbo.user_credits WHERE user_id = @adminUserId)
    BEGIN
        INSERT INTO dbo.user_credits (user_id, chat_credits, quiz_gen_credits, expires_at)
        VALUES (@adminUserId, 999999, 999999, NULL);
    END
    ELSE
    BEGIN
        -- Update existing credits to unlimited
        UPDATE dbo.user_credits
        SET chat_credits = 999999, quiz_gen_credits = 999999, expires_at = NULL
        WHERE user_id = @adminUserId;
    END;
END;

-- ============================================================================
-- 5. CREATE VIEW FOR ADMIN STATISTICS
-- ============================================================================

GO
CREATE VIEW dbo.vw_admin_dashboard_stats AS
SELECT
    -- User statistics
    (SELECT COUNT(*) FROM dbo.users) AS total_users,
    (SELECT COUNT(*) FROM dbo.users WHERE is_active = 1) AS active_users,
    (SELECT COUNT(*) FROM dbo.users WHERE is_active = 0) AS banned_users,
    (SELECT COUNT(*) FROM dbo.users WHERE created_at >= DATEADD(day, -30, GETDATE())) AS new_users_last_30_days,
    
    -- Payment statistics (if payments table exists)
    (SELECT COUNT(*) FROM dbo.payments WHERE status = 'SUCCESS') AS total_successful_payments,
    (SELECT ISNULL(SUM(amount), 0) FROM dbo.payments WHERE status = 'SUCCESS') AS total_revenue,
    (SELECT ISNULL(SUM(amount), 0) FROM dbo.payments WHERE status = 'SUCCESS' AND created_at >= DATEADD(day, -30, GETDATE())) AS revenue_last_30_days,
    
    -- Quiz statistics
    (SELECT COUNT(*) FROM dbo.quiz_sets) AS total_quiz_sets,
    (SELECT COUNT(*) FROM dbo.quiz_attempts) AS total_quiz_attempts,
    
    -- Chat statistics
    (SELECT COUNT(*) FROM dbo.chat_sessions) AS total_chat_sessions,
    (SELECT COUNT(*) FROM dbo.chat_messages WHERE role = 'USER') AS total_chat_messages,
    
    -- Legal documents
    (SELECT COUNT(*) FROM dbo.legal_documents) AS total_legal_documents,
    (SELECT COUNT(*) FROM dbo.legal_articles) AS total_legal_articles;
GO

-- ============================================================================
-- 6. ADD COMMENTS FOR DOCUMENTATION
-- ============================================================================

EXEC sp_addextendedproperty 
    @name = N'MS_Description', 
    @value = N'Indicates if user account is active (1) or banned (0)', 
    @level0type = N'SCHEMA', @level0name = 'dbo',
    @level1type = N'TABLE',  @level1name = 'users',
    @level2type = N'COLUMN', @level2name = 'is_active';

EXEC sp_addextendedproperty 
    @name = N'MS_Description', 
    @value = N'Reason why user was banned', 
    @level0type = N'SCHEMA', @level0name = 'dbo',
    @level1type = N'TABLE',  @level1name = 'users',
    @level2type = N'COLUMN', @level2name = 'ban_reason';

EXEC sp_addextendedproperty 
    @name = N'MS_Description', 
    @value = N'Timestamp when user was banned', 
    @level0type = N'SCHEMA', @level0name = 'dbo',
    @level1type = N'TABLE',  @level1name = 'users',
    @level2type = N'COLUMN', @level2name = 'banned_at';

-- ============================================================================
-- END OF MIGRATION
-- ============================================================================
