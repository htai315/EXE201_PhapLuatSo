-- Clear all PENDING payments để test lại từ đầu
-- Run this in SQL Server Management Studio

-- Option 1: Mark as EXPIRED (recommended - keep history)
UPDATE payments 
SET status = 'EXPIRED' 
WHERE status = 'PENDING';

-- Option 2: Delete (if you want to clean up completely)
-- DELETE FROM payments WHERE status = 'PENDING';

SELECT * FROM payments WHERE status = 'PENDING';
-- Should return 0 rows after running
