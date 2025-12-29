-- Fix Flyway Migration V2
-- Run this script in SQL Server Management Studio

USE EXE201_PhapLuatSo;
GO

-- 1. Delete failed migration record from flyway_schema_history
DELETE FROM flyway_schema_history WHERE version = '2';
GO

-- 2. Drop payments table if exists (from failed migration)
IF OBJECT_ID('payments', 'U') IS NOT NULL
BEGIN
    DROP TABLE payments;
END
GO

-- Done! Now you can run: ./mvnw spring-boot:run
-- The migration V2 will run successfully with the fixed schema
