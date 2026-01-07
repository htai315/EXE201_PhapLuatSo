-- V5: Add PayOS columns to payments table
-- Note: vnp_txn_ref is kept as-is for backward compatibility

-- Add order_code column for PayOS
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('payments') AND name = 'order_code')
BEGIN
    ALTER TABLE payments ADD order_code BIGINT NULL;
END
GO

-- Add transaction_id column for PayOS
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('payments') AND name = 'transaction_id')
BEGIN
    ALTER TABLE payments ADD transaction_id NVARCHAR(100) NULL;
END
GO

-- Create unique index on order_code (allow nulls)
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_payments_order_code' AND object_id = OBJECT_ID('payments'))
BEGIN
    CREATE UNIQUE INDEX IX_payments_order_code ON payments(order_code) WHERE order_code IS NOT NULL;
END
GO
