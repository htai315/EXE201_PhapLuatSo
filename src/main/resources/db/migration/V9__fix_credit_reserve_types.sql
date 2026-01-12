-- V9: Add RESERVE and CONFIRM to credit_transactions type constraint
-- This fixes the credit reservation system that was added in credit-improvements spec

-- Drop old constraint and add new one with reservation types
ALTER TABLE credit_transactions DROP CONSTRAINT IF EXISTS ck_trans_type;

ALTER TABLE credit_transactions ADD CONSTRAINT ck_trans_type 
    CHECK (type IN ('PURCHASE', 'USAGE', 'BONUS', 'REFUND', 'EXPIRE', 'ADMIN_ADD', 'ADMIN_REMOVE', 'RESERVE', 'CONFIRM'));
