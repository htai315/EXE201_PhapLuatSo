-- Xóa bảng flyway_schema_history để reset hoàn toàn
USE EXE201_PhapLuatSo;
GO

-- Kiểm tra database đang dùng
SELECT DB_NAME() AS CurrentDatabase;
GO

-- Xem các bảng hiện có
SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'dbo';
GO

-- Xóa bảng flyway nếu tồn tại
IF EXISTS (SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'flyway_schema_history')
BEGIN
    DROP TABLE flyway_schema_history;
    PRINT 'Dropped flyway_schema_history';
END
ELSE
BEGIN
    PRINT 'flyway_schema_history does not exist';
END
GO

-- Verify
SELECT 'Ready for fresh migration!' AS Status;
GO
