-- =====================================================
-- V6: Payment Idempotency Records
-- Tránh duplicate payment khi network retry
-- =====================================================

-- Table lưu idempotency keys
CREATE TABLE payment_idempotency_records (
    id BIGSERIAL PRIMARY KEY,
    
    -- Scoped key format: "{userId}:{idempotencyKey}"
    scoped_key VARCHAR(255) NOT NULL,
    
    -- User reference
    user_id BIGINT NOT NULL,
    
    -- Plan được request
    plan_code VARCHAR(50),
    
    -- Payment được tạo (nullable vì có thể chưa tạo xong)
    payment_id BIGINT,
    
    -- Status của idempotency record
    -- PENDING: đang xử lý
    -- SUCCESS: payment thành công
    -- FAILED: payment thất bại
    -- EXPIRED: payment hết hạn
    status VARCHAR(20) DEFAULT 'PENDING',
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    
    -- Constraints
    CONSTRAINT uk_idempotency_scoped_key UNIQUE (scoped_key),
    CONSTRAINT fk_idempotency_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_idempotency_payment FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE SET NULL
);

-- Index cho lookup by scoped_key (đã có unique constraint)
-- Index cho cleanup expired records
CREATE INDEX idx_idempotency_expires_at ON payment_idempotency_records(expires_at);

-- Index cho lookup by user
CREATE INDEX idx_idempotency_user_id ON payment_idempotency_records(user_id);

-- Comment
COMMENT ON TABLE payment_idempotency_records IS 'Lưu idempotency keys để tránh duplicate payment khi network retry';
COMMENT ON COLUMN payment_idempotency_records.scoped_key IS 'Format: {userId}:{idempotencyKey}';
COMMENT ON COLUMN payment_idempotency_records.expires_at IS 'Key hết hạn sau 24 giờ';
