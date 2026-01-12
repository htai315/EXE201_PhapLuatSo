-- V7: Credit System Improvements
-- Features: Optimistic Locking, Credit Reservation/Refund

-- Add version column for optimistic locking
ALTER TABLE user_credits ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 0;

-- Create credit_reservations table for reserve/confirm/refund pattern
CREATE TABLE IF NOT EXISTS credit_reservations (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    credit_type VARCHAR(20) NOT NULL,  -- CHAT, QUIZ_GEN
    amount INTEGER NOT NULL DEFAULT 1,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',  -- PENDING, CONFIRMED, REFUNDED, EXPIRED
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    confirmed_at TIMESTAMP,
    refunded_at TIMESTAMP,
    operation_type VARCHAR(50),  -- AI_CHAT, AI_QUIZ_GEN
    
    CONSTRAINT chk_reservation_status CHECK (status IN ('PENDING', 'CONFIRMED', 'REFUNDED', 'EXPIRED')),
    CONSTRAINT chk_reservation_amount CHECK (amount > 0)
);

-- Indexes for efficient queries
CREATE INDEX IF NOT EXISTS idx_credit_reservations_user_status ON credit_reservations(user_id, status);
CREATE INDEX IF NOT EXISTS idx_credit_reservations_expires ON credit_reservations(expires_at) WHERE status = 'PENDING';

-- Comment
COMMENT ON TABLE credit_reservations IS 'Tracks credit reservations for refund support when AI operations fail';
COMMENT ON COLUMN credit_reservations.status IS 'PENDING: reserved, CONFIRMED: used, REFUNDED: returned, EXPIRED: auto-refunded';
