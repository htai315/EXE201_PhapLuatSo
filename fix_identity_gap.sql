-- Fix IDENTITY gap in users table
-- Run this in SQL Server Management Studio or Azure Data Studio

-- Step 1: Check current max ID
SELECT MAX(id) as current_max_id FROM users;

-- Step 2: Reseed to continue from current max
-- Replace 7 with the actual max ID from step 1
DBCC CHECKIDENT ('users', RESEED, 7);

-- Step 3: Disable IDENTITY cache to prevent future gaps (optional, for development only)
ALTER DATABASE EXE201_PhapLuatSo SET IDENTITY_CACHE OFF;

-- Step 4: Verify next ID will be correct
-- Next insert will get ID = 8
SELECT IDENT_CURRENT('users') as current_identity_value;

-- IMPORTANT NOTES:
-- 1. IDENTITY gaps are NORMAL in production and should be accepted
-- 2. Disabling IDENTITY_CACHE may impact performance in high-volume scenarios
-- 3. Only use RESEED in development/testing environments
-- 4. In production, gaps in IDs are expected and safe
