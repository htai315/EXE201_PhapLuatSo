-- Chạy script này trong SQL Server Management Studio hoặc Azure Data Studio
-- để xóa migration V8 bị lỗi và cho phép Flyway chạy lại

USE AIPhapLuatSo;
GO

-- Xóa bản ghi migration V8 bị lỗi
DELETE FROM dbo.flyway_schema_history 
WHERE version = '8';
GO

-- Kiểm tra lại
SELECT * FROM dbo.flyway_schema_history 
ORDER BY installed_rank DESC;
GO
