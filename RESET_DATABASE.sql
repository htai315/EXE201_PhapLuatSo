-- Reset Database EXE201_PhapLuatSo
-- Chạy file này trong SQL Server Management Studio

USE master;
GO

-- Drop database nếu tồn tại
IF EXISTS (SELECT name FROM sys.databases WHERE name = 'EXE201_PhapLuatSo')
BEGIN
    ALTER DATABASE EXE201_PhapLuatSo SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    DROP DATABASE EXE201_PhapLuatSo;
END
GO

-- Tạo database mới
CREATE DATABASE EXE201_PhapLuatSo;
GO

-- Verify
USE EXE201_PhapLuatSo;
GO

SELECT 'Database EXE201_PhapLuatSo created successfully!' AS Status;
GO
