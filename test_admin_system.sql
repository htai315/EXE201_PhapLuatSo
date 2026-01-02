-- ============================================================================
-- SCRIPT KIỂM TRA HỆ THỐNG ADMIN
-- Chạy script này để kiểm tra xem database đã sẵn sàng chưa
-- ============================================================================

PRINT '========================================';
PRINT 'KIỂM TRA HỆ THỐNG ADMIN';
PRINT '========================================';
PRINT '';

-- ============================================================================
-- 1. KIỂM TRA ROLES
-- ============================================================================
PRINT '1. Kiểm tra Roles:';
PRINT '-------------------';

IF EXISTS (SELECT 1 FROM dbo.roles WHERE name = 'USER')
    PRINT '✓ Role USER tồn tại'
ELSE
    PRINT '✗ Role USER KHÔNG tồn tại - CẦN TẠO!';

IF EXISTS (SELECT 1 FROM dbo.roles WHERE name = 'ADMIN')
    PRINT '✓ Role ADMIN tồn tại'
ELSE
    PRINT '✗ Role ADMIN KHÔNG tồn tại - CẦN TẠO!';

PRINT '';
PRINT 'Danh sách roles hiện có:';
SELECT id, name FROM dbo.roles;
PRINT '';

-- ============================================================================
-- 2. KIỂM TRA ADMIN USER
-- ============================================================================
PRINT '2. Kiểm tra Admin User:';
PRINT '------------------------';

IF EXISTS (SELECT 1 FROM dbo.users WHERE email = 'admin@phapluatso.vn')
BEGIN
    PRINT '✓ Admin user tồn tại';
    
    -- Kiểm tra password hash
    DECLARE @passwordHash NVARCHAR(255);
    SELECT @passwordHash = password_hash FROM dbo.users WHERE email = 'admin@phapluatso.vn';
    
    IF @passwordHash = '$2a$10$placeholder'
        PRINT '⚠ Password vẫn là PLACEHOLDER - CẦN ĐỔI PASSWORD!';
    ELSE
        PRINT '✓ Password đã được set';
    
    -- Kiểm tra role ADMIN
    IF EXISTS (
        SELECT 1 FROM dbo.users u
        JOIN dbo.user_roles ur ON u.id = ur.user_id
        JOIN dbo.roles r ON ur.role_id = r.id
        WHERE u.email = 'admin@phapluatso.vn' AND r.name = 'ADMIN'
    )
        PRINT '✓ Admin user có role ADMIN';
    ELSE
        PRINT '✗ Admin user KHÔNG có role ADMIN - CẦN THÊM!';
    
    -- Hiển thị thông tin admin
    PRINT '';
    PRINT 'Thông tin admin user:';
    SELECT 
        u.id,
        u.email,
        u.full_name,
        u.is_enabled,
        u.is_active,
        u.created_at,
        STRING_AGG(r.name, ', ') AS roles
    FROM dbo.users u
    LEFT JOIN dbo.user_roles ur ON u.id = ur.user_id
    LEFT JOIN dbo.roles r ON ur.role_id = r.id
    WHERE u.email = 'admin@phapluatso.vn'
    GROUP BY u.id, u.email, u.full_name, u.is_enabled, u.is_active, u.created_at;
END
ELSE
BEGIN
    PRINT '✗ Admin user KHÔNG tồn tại - CẦN TẠO!';
END

PRINT '';

-- ============================================================================
-- 3. KIỂM TRA ADMIN FIELDS TRONG USERS TABLE
-- ============================================================================
PRINT '3. Kiểm tra Admin Fields:';
PRINT '--------------------------';

IF EXISTS (
    SELECT 1 FROM sys.columns 
    WHERE object_id = OBJECT_ID('dbo.users') AND name = 'is_active'
)
    PRINT '✓ Field is_active tồn tại'
ELSE
    PRINT '✗ Field is_active KHÔNG tồn tại - CẦN CHẠY MIGRATION!';

IF EXISTS (
    SELECT 1 FROM sys.columns 
    WHERE object_id = OBJECT_ID('dbo.users') AND name = 'ban_reason'
)
    PRINT '✓ Field ban_reason tồn tại'
ELSE
    PRINT '✗ Field ban_reason KHÔNG tồn tại - CẦN CHẠY MIGRATION!';

IF EXISTS (
    SELECT 1 FROM sys.columns 
    WHERE object_id = OBJECT_ID('dbo.users') AND name = 'banned_at'
)
    PRINT '✓ Field banned_at tồn tại'
ELSE
    PRINT '✗ Field banned_at KHÔNG tồn tại - CẦN CHẠY MIGRATION!';

IF EXISTS (
    SELECT 1 FROM sys.columns 
    WHERE object_id = OBJECT_ID('dbo.users') AND name = 'banned_by'
)
    PRINT '✓ Field banned_by tồn tại'
ELSE
    PRINT '✗ Field banned_by KHÔNG tồn tại - CẦN CHẠY MIGRATION!';

PRINT '';

-- ============================================================================
-- 4. KIỂM TRA ADMIN ACTIVITY LOGS TABLE
-- ============================================================================
PRINT '4. Kiểm tra Activity Logs Table:';
PRINT '---------------------------------';

IF EXISTS (SELECT 1 FROM sys.tables WHERE name = 'admin_activity_logs')
BEGIN
    PRINT '✓ Table admin_activity_logs tồn tại';
    
    DECLARE @logCount INT;
    SELECT @logCount = COUNT(*) FROM dbo.admin_activity_logs;
    PRINT '  Số lượng logs: ' + CAST(@logCount AS NVARCHAR(10));
END
ELSE
    PRINT '✗ Table admin_activity_logs KHÔNG tồn tại - CẦN CHẠY MIGRATION!';

PRINT '';

-- ============================================================================
-- 5. KIỂM TRA INDEXES
-- ============================================================================
PRINT '5. Kiểm tra Indexes:';
PRINT '--------------------';

IF EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_users_is_active')
    PRINT '✓ Index ix_users_is_active tồn tại'
ELSE
    PRINT '⚠ Index ix_users_is_active không tồn tại';

IF EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_users_created_at')
    PRINT '✓ Index ix_users_created_at tồn tại'
ELSE
    PRINT '⚠ Index ix_users_created_at không tồn tại';

IF EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_admin_logs_created_at')
    PRINT '✓ Index ix_admin_logs_created_at tồn tại'
ELSE
    PRINT '⚠ Index ix_admin_logs_created_at không tồn tại';

PRINT '';

-- ============================================================================
-- 6. KIỂM TRA VIEW
-- ============================================================================
PRINT '6. Kiểm tra View:';
PRINT '-----------------';

IF EXISTS (SELECT 1 FROM sys.views WHERE name = 'vw_admin_dashboard_stats')
BEGIN
    PRINT '✓ View vw_admin_dashboard_stats tồn tại';
    PRINT '';
    PRINT 'Dashboard Statistics:';
    SELECT * FROM dbo.vw_admin_dashboard_stats;
END
ELSE
    PRINT '⚠ View vw_admin_dashboard_stats không tồn tại';

PRINT '';

-- ============================================================================
-- 7. THỐNG KÊ USERS
-- ============================================================================
PRINT '7. Thống kê Users:';
PRINT '------------------';

DECLARE @totalUsers INT, @activeUsers INT, @bannedUsers INT, @adminUsers INT;

SELECT @totalUsers = COUNT(*) FROM dbo.users;
SELECT @activeUsers = COUNT(*) FROM dbo.users WHERE is_active = 1;
SELECT @bannedUsers = COUNT(*) FROM dbo.users WHERE is_active = 0;
SELECT @adminUsers = COUNT(DISTINCT u.id) 
FROM dbo.users u
JOIN dbo.user_roles ur ON u.id = ur.user_id
JOIN dbo.roles r ON ur.role_id = r.id
WHERE r.name = 'ADMIN';

PRINT 'Tổng users: ' + CAST(@totalUsers AS NVARCHAR(10));
PRINT 'Users active: ' + CAST(@activeUsers AS NVARCHAR(10));
PRINT 'Users banned: ' + CAST(@bannedUsers AS NVARCHAR(10));
PRINT 'Admin users: ' + CAST(@adminUsers AS NVARCHAR(10));

PRINT '';

-- ============================================================================
-- 8. DANH SÁCH ADMIN USERS
-- ============================================================================
PRINT '8. Danh sách Admin Users:';
PRINT '-------------------------';

SELECT 
    u.id,
    u.email,
    u.full_name,
    u.provider,
    u.is_enabled,
    u.is_active,
    u.created_at
FROM dbo.users u
JOIN dbo.user_roles ur ON u.id = ur.user_id
JOIN dbo.roles r ON ur.role_id = r.id
WHERE r.name = 'ADMIN'
ORDER BY u.created_at DESC;

PRINT '';

-- ============================================================================
-- KẾT LUẬN
-- ============================================================================
PRINT '========================================';
PRINT 'KẾT LUẬN:';
PRINT '========================================';

DECLARE @issues INT = 0;

-- Đếm số vấn đề
IF NOT EXISTS (SELECT 1 FROM dbo.roles WHERE name = 'USER')
    SET @issues = @issues + 1;
IF NOT EXISTS (SELECT 1 FROM dbo.roles WHERE name = 'ADMIN')
    SET @issues = @issues + 1;
IF NOT EXISTS (SELECT 1 FROM dbo.users WHERE email = 'admin@phapluatso.vn')
    SET @issues = @issues + 1;
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'admin_activity_logs')
    SET @issues = @issues + 1;
IF NOT EXISTS (
    SELECT 1 FROM sys.columns 
    WHERE object_id = OBJECT_ID('dbo.users') AND name = 'is_active'
)
    SET @issues = @issues + 1;

IF @issues = 0
BEGIN
    PRINT '✓ HỆ THỐNG ADMIN SẴN SÀNG!';
    PRINT '';
    PRINT 'Bạn có thể:';
    PRINT '1. Login với admin account';
    PRINT '2. Truy cập /html/admin/dashboard.html';
    PRINT '3. Quản lý users, payments, activity logs';
END
ELSE
BEGIN
    PRINT '⚠ CÓ ' + CAST(@issues AS NVARCHAR(10)) + ' VẤN ĐỀ CẦN KHẮC PHỤC!';
    PRINT '';
    PRINT 'Hãy xem lại các mục trên và:';
    PRINT '1. Chạy migration V4__add_admin_features.sql nếu chưa chạy';
    PRINT '2. Tạo roles USER và ADMIN nếu chưa có';
    PRINT '3. Tạo admin user và set password';
END

PRINT '';
PRINT '========================================';
