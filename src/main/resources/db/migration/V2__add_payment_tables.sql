-- Payment transactions table
CREATE TABLE payments (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    plan_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    
    -- VNPay fields
    vnp_txn_ref VARCHAR(100) UNIQUE NOT NULL,
    vnp_transaction_no VARCHAR(100),
    vnp_bank_code VARCHAR(20),
    vnp_card_type VARCHAR(20),
    
    -- Status
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payment_method VARCHAR(20) DEFAULT 'VNPAY',
    
    -- Timestamps
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    paid_at DATETIME2,
    
    -- Metadata
    ip_address VARCHAR(50),
    
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (plan_id) REFERENCES plans(id)
);

CREATE INDEX idx_payments_user_id ON payments(user_id);
CREATE INDEX idx_payments_plan_id ON payments(plan_id);
CREATE INDEX idx_payments_vnp_txn_ref ON payments(vnp_txn_ref);
CREATE INDEX idx_payments_status ON payments(status);
