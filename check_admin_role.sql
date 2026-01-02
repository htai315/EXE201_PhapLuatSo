-- Script để kiểm tra và thêm role ADMIN cho user
-- Chạy script này trong SQL Server Management Studio hoặc Azure Data Studio

-- 1. Kiểm tra các roles hiện có
SELECT * FROM roles;

-- 2. Kiểm tra user và roles của họ
SELECT u.id, u.email, u.full_name, r.name as role_name
FROM users u
LEFT JOIN user_roles ur ON u.id = ur.user_id
LEFT JOIN roles r ON ur.role_id = r.id
ORDER BY u.id;

-- 3. Tìm user cần promote lên ADMIN (thay email của bạn vào đây)
-- SELECT * FROM users WHERE email = 'your-admin-email@example.com';

-- 4. Thêm role ADMIN nếu chưa có
-- Đầu tiên, kiểm tra xem role ADMIN đã tồn tại chưa
IF NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ADMIN')
BEGIN
    INSERT INTO roles (name) VALUES ('ADMIN');
    PRINT 'Created ADMIN role';
END

-- 5. Promote user lên ADMIN (thay email của bạn vào đây)
-- DECLARE @userId BIGINT;
-- DECLARE @adminRoleId BIGINT;
-- 
-- SELECT @userId = id FROM users WHERE email = 'your-admin-email@example.com';
-- SELECT @adminRoleId = id FROM roles WHERE name = 'ADMIN';
-- 
-- IF @userId IS NOT NULL AND @adminRoleId IS NOT NULL
-- BEGIN
--     IF NOT EXISTS (SELECT 1 FROM user_roles WHERE user_id = @userId AND role_id = @adminRoleId)
--     BEGIN
--         INSERT INTO user_roles (user_id, role_id) VALUES (@userId, @adminRoleId);
--         PRINT 'User promoted to ADMIN';
--     END
--     ELSE
--     BEGIN
--         PRINT 'User already has ADMIN role';
--     END
-- END

-- 6. Kiểm tra lại sau khi promote
-- SELECT u.id, u.email, u.full_name, r.name as role_name
-- FROM users u
-- LEFT JOIN user_roles ur ON u.id = ur.user_id
-- LEFT JOIN roles r ON ur.role_id = r.id
-- WHERE u.email = 'your-admin-email@example.com';
