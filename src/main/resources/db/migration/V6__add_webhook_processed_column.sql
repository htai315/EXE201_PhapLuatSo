-- V6: Add webhook_processed column for idempotency check

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('payments') AND name = 'webhook_processed')
BEGIN
    ALTER TABLE payments ADD webhook_processed BIT DEFAULT 0;
END
GO

-- Update existing SUCCESS payments to mark as processed
UPDATE payments SET webhook_processed = 1 WHERE status = 'SUCCESS';
GO
