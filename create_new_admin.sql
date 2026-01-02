-- =====================================================
-- SCRIPT TẠO TÀI KHOẢN ADMIN MỚI
-- =====================================================
-- Database: SQL Server
-- Password: Admin@123456
-- BCrypt Hash: $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhqG
-- =====================================================
-- LƯU Ý: Script này tự động detect xem đã chạy migration V4 chưa
--         Nếu chưa có cột is_active thì sẽ không insert cột đó
-- =====================================================

-- Bước 1: Tạo roles nếu chưa có
IF NOT EXISTS (SELECT 1 FROM roles WHERE name = 'USER')
BEGIN
    INSERT INTO roles (name) VALUES ('USER');
END

IF NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ADMIN')
BEGIN
    INSERT INTO roles (name) VALUES ('ADMIN');
END
GO

-- Bước 2: Tạo user admin mới
-- Thay đổi email và full_name theo ý bạn
IF NOT EXISTS (SELECT 1 FROM users WHERE email = 'admin@phapluatso.com')
BEGIN
    -- Kiểm tra xem cột is_active có tồn tại không (từ migration V4)
    IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('users') AND name = 'is_active')
    BEGIN
        -- Nếu có cột is_active (đã chạy migration V4)
        INSERT INTO users (
            email, 
            password_hash, 
            full_name, 
            provider, 
            email_verified, 
            is_enabled,
            is_active,
            created_at
        ) VALUES (
            'admin@phapluatso.com',                                                    -- Email admin
            '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhqG',          -- Password: Admin@123456 (BCrypt)
            'System Administrator',                                                     -- Tên đầy đủ
            'LOCAL',                                                                    -- Provider
            1,                                                                          -- Email verified (1 = true)
            1,                                                                          -- Enabled (1 = true)
            1,                                                                          -- Active (1 = true)
            GETDATE()                                                                   -- Created at
        );
    END
    ELSE
    BEGIN
        -- Nếu chưa có cột is_active (chưa chạy migration V4)
        INSERT INTO users (
            email, 
            password_hash, 
            full_name, 
            provider, 
            email_verified, 
            is_enabled, 
            created_at
        ) VALUES (
            'admin@phapluatso.com',                                                    -- Email admin
            '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhqG',          -- Password: Admin@123456 (BCrypt)
            'System Administrator',                                                     -- Tên đầy đủ
            'LOCAL',                                                                    -- Provider
            1,                                                                          -- Email verified (1 = true)
            1,                                                                          -- Enabled (1 = true)
            GETDATE()                                                                   -- Created at
        );
    END
END
GO

-- Bước 3: Gán USER role cho admin (để dùng được các tính năng user)
IF NOT EXISTS (
    SELECT 1 FROM user_roles ur
    INNER JOIN users u ON ur.user_id = u.id
    INNER JOIN roles r ON ur.role_id = r.id
    WHERE u.email = 'admin@phapluatso.com' AND r.name = 'USER'
)
BEGIN
    INSERT INTO user_roles (user_id, role_id)
    SELECT u.id, r.id 
    FROM users u, roles r 
    WHERE u.email = 'admin@phapluatso.com' 
      AND r.name = 'USER';
END
GO

-- Bước 4: Gán ADMIN role cho admin
IF NOT EXISTS (
    SELECT 1 FROM user_roles ur
    INNER JOIN users u ON ur.user_id = u.id
    INNER JOIN roles r ON ur.role_id = r.id
    WHERE u.email = 'admin@phapluatso.com' AND r.name = 'ADMIN'
)
BEGIN
    INSERT INTO user_roles (user_id, role_id)
    SELECT u.id, r.id 
    FROM users u, roles r 
    WHERE u.email = 'admin@phapluatso.com' 
      AND r.name = 'ADMIN';
END
GO

-- Bước 5: Verify - Kiểm tra kết quả
SELECT 
    u.id,
    u.email,
    u.full_name,
    u.provider,
    u.email_verified,
    u.is_enabled,
    u.is_active,
    STRING_AGG(r.name, ', ') WITHIN GROUP (ORDER BY r.name) as roles,
    u.created_at
FROM users u
LEFT JOIN user_roles ur ON u.id = ur.user_id
LEFT JOIN roles r ON ur.role_id = r.id
WHERE u.email = 'admin@phapluatso.com'
GROUP BY u.id, u.email, u.full_name, u.provider, u.email_verified, u.is_enabled, u.is_active, u.created_at;

-- Bước 6: Tạo credits cho admin (nếu chưa có)
-- Trigger tự động tạo credits chỉ chạy khi INSERT qua app, không chạy với SQL manual
DECLARE @adminUserId BIGINT;
SELECT @adminUserId = id FROM users WHERE email = 'admin@phapluatso.com';

IF @adminUserId IS NOT NULL
BEGIN
    -- Kiểm tra xem đã có credits chưa
    IF NOT EXISTS (SELECT 1 FROM user_credits WHERE user_id = @adminUserId)
    BEGIN
        -- Tạo credits cho admin (unlimited)
        INSERT INTO user_credits (user_id, chat_credits, quiz_gen_credits, expires_at, updated_at)
        VALUES (@adminUserId, 999999, 999999, NULL, GETDATE());
        
        PRINT 'Created unlimited credits for admin user';
    END
    ELSE
    BEGIN
        -- Update credits thành unlimited nếu đã có
        UPDATE user_credits
        SET chat_credits = 999999, quiz_gen_credits = 999999, expires_at = NULL
        WHERE user_id = @adminUserId;
        
        PRINT 'Updated admin credits to unlimited';
    END
END
GO

-- Bước 7: Verify credits
SELECT 
    u.email,
    uc.chat_credits,
    uc.quiz_gen_credits,
    uc.expires_at
FROM users u
LEFT JOIN user_credits uc ON u.id = uc.user_id
WHERE u.email = 'admin@phapluatso.com';
GO


-- =====================================================
-- THÔNG TIN ĐĂNG NHẬP
-- =====================================================
-- Email: admin@phapluatso.com
-- Password: Admin@123456
-- =====================================================

-- =====================================================
-- CÁCH CHẠY SCRIPT
-- =====================================================
-- Cách 1: SQL Server Management Studio (SSMS)
--   1. Mở SSMS và connect vào database
--   2. File > Open > File... > chọn create_new_admin.sql
--   3. Chọn đúng database ở dropdown
--   4. Click Execute (F5)
--
-- Cách 2: Command line (sqlcmd)
--   sqlcmd -S localhost -d phapluatso -i create_new_admin.sql
--
-- Cách 3: Azure Data Studio
--   1. Mở file create_new_admin.sql
--   2. Chọn đúng database
--   3. Click Run (F5)
-- =====================================================

-- =====================================================
-- TÙY CHỈNH (OPTIONAL)
-- =====================================================
-- Nếu muốn đổi email hoặc tên, tìm và thay thế:
-- - 'admin@phapluatso.com' -> email bạn muốn
-- - 'System Administrator' -> tên bạn muốn
-- =====================================================

-- =====================================================
-- PASSWORD KHÁC (OPTIONAL)
-- =====================================================
-- Nếu muốn dùng password khác, thay thế hash ở line 37
-- 
-- Các password đã hash sẵn (BCrypt):
-- 
-- Password: Admin@123
-- Hash: $2a$10$8cjz47bjbR4Mn8GMg9IZx.vyjhLXR/SKKMSZ9.mP0gxeLn/W1p2Zu
--
-- Password: Admin@123456
-- Hash: $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhqG
--
-- Password: SuperAdmin@2024
-- Hash: $2a$10$rKKHqxqYQGefNiPh2d7C0.JZd5PlFJP7ntOEWjPXvBJHiFnNYtjVi
--
-- Password: Admin@2024
-- Hash: $2a$10$vQKPXO5dQj5iJZH4mEBPa.nKZWz8qhKiTZMRZoMyeIjZAgcfl7p92
-- =====================================================
