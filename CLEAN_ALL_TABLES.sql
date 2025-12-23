-- Xóa toàn bộ tables trong database EXE201_PhapLuatSo
USE EXE201_PhapLuatSo;
GO

-- Disable all constraints
EXEC sp_MSforeachtable 'ALTER TABLE ? NOCHECK CONSTRAINT ALL';
GO

-- Drop all tables
DECLARE @sql NVARCHAR(MAX) = '';
SELECT @sql += 'DROP TABLE IF EXISTS ' + QUOTENAME(TABLE_SCHEMA) + '.' + QUOTENAME(TABLE_NAME) + ';' + CHAR(13)
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_TYPE = 'BASE TABLE' AND TABLE_SCHEMA = 'dbo';

EXEC sp_executesql @sql;
GO

-- Verify
SELECT 'All tables dropped!' AS Status;
SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'dbo';
GO
