-- ============================================================================
-- SESSION-BASED BILLING FOR LEGAL AI CHAT
-- V2 Migration: Add session billing support with backfill
-- ============================================================================

-- Add session billing fields to chat_sessions
ALTER TABLE chat_sessions
ADD COLUMN user_question_count INT NOT NULL DEFAULT 0,
ADD COLUMN charge_state VARCHAR(20) NOT NULL DEFAULT 'NOT_CHARGED',
ADD COLUMN charge_reservation_id BIGINT NULL,
ADD COLUMN version INT NOT NULL DEFAULT 0;

-- Add session reference to credit reservations
ALTER TABLE credit_reservations
ADD COLUMN session_id BIGINT NULL;

-- Add constraints for data integrity
ALTER TABLE chat_sessions
ADD CONSTRAINT chk_chat_sessions_question_count
    CHECK (user_question_count BETWEEN 0 AND 10),
ADD CONSTRAINT chk_chat_sessions_charge_state
    CHECK (charge_state IN ('NOT_CHARGED', 'CHARGING', 'CHARGED')),
ADD CONSTRAINT fk_chat_sessions_charge_reservation
    FOREIGN KEY (charge_reservation_id) REFERENCES credit_reservations(id);

-- Add foreign key for session reservations
ALTER TABLE credit_reservations
ADD CONSTRAINT fk_credit_reservations_session
    FOREIGN KEY (session_id) REFERENCES chat_sessions(id) ON DELETE CASCADE;

-- Add indexes for performance and constraints
CREATE INDEX ix_chat_messages_session_role ON chat_messages(session_id, role);

CREATE UNIQUE INDEX ux_reservation_pending_session
ON credit_reservations(user_id, session_id)
WHERE status = 'PENDING' AND session_id IS NOT NULL;

-- ============================================================================
-- BACKFILL EXISTING SESSIONS
-- ============================================================================

-- Update existing sessions with question counts
UPDATE chat_sessions
SET user_question_count = LEAST(10, COALESCE((
    SELECT COUNT(*)
    FROM chat_messages
    WHERE session_id = chat_sessions.id
    AND role = 'USER'
), 0));

-- Mark sessions with questions as CHARGED (already "used")
UPDATE chat_sessions
SET charge_state = 'CHARGED'
WHERE user_question_count > 0;

-- ============================================================================
-- EXTEND CREDIT TRANSACTION TYPES
-- ============================================================================

-- Update check constraint to include session-based operations
ALTER TABLE credit_transactions
DROP CONSTRAINT ck_trans_type;

ALTER TABLE credit_transactions
ADD CONSTRAINT ck_trans_type CHECK (type IN ('PURCHASE', 'USAGE', 'BONUS', 'REFUND', 'EXPIRE', 'ADMIN_ADD', 'ADMIN_REMOVE', 'RESERVE', 'CONFIRM'));

-- ============================================================================
-- COMMENTS FOR DOCUMENTATION
-- ============================================================================

COMMENT ON COLUMN chat_sessions.user_question_count IS 'Count of user questions sent in this session (0-10)';
COMMENT ON COLUMN chat_sessions.charge_state IS 'Session charging state: NOT_CHARGED, CHARGING, CHARGED';
COMMENT ON COLUMN chat_sessions.charge_reservation_id IS 'ID of the credit reservation used to charge this session';
COMMENT ON COLUMN chat_sessions.version IS 'Optimistic locking version for concurrent updates';
COMMENT ON COLUMN credit_reservations.session_id IS 'Session this reservation is associated with (for session billing)';

-- ============================================================================
-- END OF V2 MIGRATION
-- ============================================================================