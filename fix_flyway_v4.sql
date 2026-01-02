-- Fix Flyway V4 migration failure
-- Run this script manually in SQL Server Management Studio

USE EXE201_PhapLuatSo;
GO

-- 1. Check current Flyway history
SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC;
GO

-- 2. Delete failed V4 migration
DELETE FROM flyway_schema_history WHERE version = '4' AND success = 0;
GO

-- 3. Verify deletion
SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC;
GO

-- Now you can restart the application and V4 will run again
