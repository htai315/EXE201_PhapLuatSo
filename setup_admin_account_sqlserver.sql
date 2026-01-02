-- Setup Admin Account for Pháp Luật Số (SQL Server)
-- Run this script after database initialization

-- Step 1: Ensure ADMIN role exists
IF NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ADMIN')
BEGIN
    INSERT INTO roles (name) VALUES ('ADMIN');
END

-- Step 2: Ensure USER role exists
IF NOT EXISTS (SELECT 1 FROM roles WHERE name = 'USER')
BEGIN
    INSERT INTO roles (name) VALUES ('USER');
END

-- Step 3: Create admin user if not exists
-- Password: admin123 (BCrypt hashed)
-- BCrypt hash: $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
IF NOT EXISTS (SELECT 1 FROM users WHERE email = 'admin@phapluatso.vn')
BEGIN
    INSERT INTO users (email, password_hash, full_name, is_enabled, email_verified, is_active, created_at, provider)
    VALUES ('admin@phapluatso.vn', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Administrator', 1, 1, 1, GETDATE(), 'LOCAL');
END

-- Step 4: Assign ADMIN role to admin user
DECLARE @adminUserId INT;
DECLARE @adminRoleId INT;
DECLARE @userRoleId INT;

SELECT @adminUserId = id FROM users WHERE email = 'admin@phapluatso.vn';
SELECT @adminRoleId = id FROM roles WHERE name = 'ADMIN';
SELECT @userRoleId = id FROM roles WHERE name = 'USER';

-- Assign ADMIN role
IF NOT EXISTS (SELECT 1 FROM user_roles WHERE user_id = @adminUserId AND role_id = @adminRoleId)
BEGIN
    INSERT INTO user_roles (user_id, role_id) VALUES (@adminUserId, @adminRoleId);
END

-- Assign USER role (users can have multiple roles)
IF NOT EXISTS (SELECT 1 FROM user_roles WHERE user_id = @adminUserId AND role_id = @userRoleId)
BEGIN
    INSERT INTO user_roles (user_id, role_id) VALUES (@adminUserId, @userRoleId);
END

-- Step 5: Verify the setup
SELECT 
    u.id,
    u.email,
    u.full_name,
    u.is_active,
    u.is_enabled,
    STRING_AGG(r.name, ', ') as roles
FROM users u
LEFT JOIN user_roles ur ON u.id = ur.user_id
LEFT JOIN roles r ON ur.role_id = r.id
WHERE u.email = 'admin@phapluatso.vn'
GROUP BY u.id, u.email, u.full_name, u.is_active, u.is_enabled;

-- IMPORTANT NOTES:
-- 1. Default admin credentials:
--    Email: admin@phapluatso.vn
--    Password: admin123
-- 
-- 2. CHANGE THE PASSWORD IMMEDIATELY after first login!
--
-- 3. To create additional admin accounts:
--    - Register through the app first
--    - Then run this query to promote to admin:
--      INSERT INTO user_roles (user_id, role_id)
--      SELECT u.id, r.id FROM users u, roles r 
--      WHERE u.email = 'new-admin@example.com' AND r.name = 'ADMIN';
