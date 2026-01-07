-- Run this script in SQL Server Management Studio to fix the migration

-- 1. Delete failed migration record from flyway history
DELETE FROM flyway_schema_history WHERE version = '5';

-- 2. Drop unique constraint on vnp_txn_ref (find and drop)
DECLARE @constraintName NVARCHAR(200);

-- Find unique constraint
SELECT @constraintName = kc.name 
FROM sys.key_constraints kc
INNER JOIN sys.index_columns ic ON kc.parent_object_id = ic.object_id AND kc.unique_index_id = ic.index_id
INNER JOIN sys.columns c ON ic.object_id = c.object_id AND ic.column_id = c.column_id
WHERE kc.parent_object_id = OBJECT_ID('payments') 
AND kc.type = 'UQ' 
AND c.name = 'vnp_txn_ref';

IF @constraintName IS NOT NULL
BEGIN
    PRINT 'Dropping constraint: ' + @constraintName;
    EXEC('ALTER TABLE payments DROP CONSTRAINT [' + @constraintName + ']');
END

-- Also find and drop unique index
DECLARE @indexName NVARCHAR(200);
SELECT @indexName = i.name 
FROM sys.indexes i
INNER JOIN sys.index_columns ic ON i.object_id = ic.object_id AND i.index_id = ic.index_id
INNER JOIN sys.columns c ON ic.object_id = c.object_id AND ic.column_id = c.column_id
WHERE i.object_id = OBJECT_ID('payments') 
AND c.name = 'vnp_txn_ref' 
AND i.is_unique = 1
AND i.is_primary_key = 0;

IF @indexName IS NOT NULL
BEGIN
    PRINT 'Dropping index: ' + @indexName;
    EXEC('DROP INDEX [' + @indexName + '] ON payments');
END

-- 3. Make vnp_txn_ref nullable
ALTER TABLE payments ALTER COLUMN vnp_txn_ref NVARCHAR(100) NULL;

-- 4. Add order_code column
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('payments') AND name = 'order_code')
BEGIN
    ALTER TABLE payments ADD order_code BIGINT NULL;
    PRINT 'Added order_code column';
END

-- 5. Add transaction_id column
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('payments') AND name = 'transaction_id')
BEGIN
    ALTER TABLE payments ADD transaction_id NVARCHAR(100) NULL;
    PRINT 'Added transaction_id column';
END

-- 6. Create unique index on order_code
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_payments_order_code' AND object_id = OBJECT_ID('payments'))
BEGIN
    CREATE UNIQUE INDEX IX_payments_order_code ON payments(order_code) WHERE order_code IS NOT NULL;
    PRINT 'Created index IX_payments_order_code';
END

-- 7. Insert migration record manually
INSERT INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
VALUES (
    (SELECT ISNULL(MAX(installed_rank), 0) + 1 FROM flyway_schema_history),
    '5',
    'add payos columns',
    'SQL',
    'V5__add_payos_columns.sql',
    NULL,
    'sa',
    GETDATE(),
    100,
    1
);

PRINT 'Migration V5 completed successfully!';
