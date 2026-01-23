-- ============================================================================
-- V4: Payment System Production-Ready Migration (SAFE ORDER)
-- ============================================================================

-- 1) Add credit_retry_count column (idempotent)
ALTER TABLE payments
ADD COLUMN IF NOT EXISTS credit_retry_count INTEGER DEFAULT 0;

-- 2) DATA CLEANUP must run BEFORE adding CHECK constraint
-- 2.1) Legacy SUCCESS -> CREDITED
UPDATE payments
SET status = 'CREDITED'
WHERE status = 'SUCCESS';

UPDATE payment_idempotency_records
SET status = 'CREDITED'
WHERE status = 'SUCCESS';

-- 2.2) Quarantine any unexpected statuses to NEEDS_REVIEW (prevents CHECK failure)
-- (This is optional but strongly recommended for production safety)
UPDATE payments
SET status = 'NEEDS_REVIEW'
WHERE status IS NOT NULL
  AND status NOT IN (
    'PENDING',
    'PAID',
    'CREDITED',
    'PAID_CREDIT_FAILED',
    'FAILED',
    'CANCELLED',
    'EXPIRED',
    'NEEDS_REVIEW'
  );

-- 3) Drop existing constraints (idempotent)
ALTER TABLE payments DROP CONSTRAINT IF EXISTS ck_payment_status;
ALTER TABLE payments DROP CONSTRAINT IF EXISTS chk_payment_status;

-- 4) Add CHECK constraint (enforce state machine)
ALTER TABLE payments
ADD CONSTRAINT ck_payment_status
CHECK (status IN (
    'PENDING',
    'PAID',
    'CREDITED',
    'PAID_CREDIT_FAILED',
    'FAILED',
    'CANCELLED',
    'EXPIRED',
    'NEEDS_REVIEW'
));

-- 5) Index for retry job (idempotent)
CREATE INDEX IF NOT EXISTS ix_payments_credit_retry
ON payments(status, credit_retry_count)
WHERE status = 'PAID_CREDIT_FAILED';

-- 6) Comments
COMMENT ON COLUMN payments.credit_retry_count IS
'Number of retry attempts to add credits after payment received';

COMMENT ON COLUMN payments.status IS
'Payment status: PENDING→PAID→CREDITED (success) or PENDING→FAILED (no payment)';

COMMENT ON CONSTRAINT ck_payment_status ON payments IS
'Ensures payment status follows state machine: PENDING, PAID, CREDITED, PAID_CREDIT_FAILED, FAILED, CANCELLED, EXPIRED, NEEDS_REVIEW';

-- ============================================================================
-- END OF V4 MIGRATION
-- ============================================================================
