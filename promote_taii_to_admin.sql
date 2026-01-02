-- Promote taii@gmail.com to ADMIN
-- Run this in SQL Server Management Studio or Azure Data Studio

-- Step 1: Verify the user exists
SELECT id, email, full_name FROM users WHERE email = 'taii@gmail.com';

-- Step 2: Get role IDs
SELECT id, name FROM roles;

-- Step 3: Assign ADMIN role to taii@gmail.com
DECLARE @userId INT;
DECLARE @adminRoleId INT;

SELECT @userId = id FROM users WHERE email = 'taii@gmail.com';
SELECT @adminRoleId = id FROM roles WHERE name = 'ADMIN';

-- Check if already has ADMIN role
IF NOT EXISTS (SELECT 1 FROM user_roles WHERE user_id = @userId AND role_id = @adminRoleId)
BEGIN
    INSERT INTO user_roles (user_id, role_id) 
    VALUES (@userId, @adminRoleId);
    PRINT 'ADMIN role assigned to taii@gmail.com successfully!';
END
ELSE
BEGIN
    PRINT 'User already has ADMIN role!';
END

-- Step 4: Verify the assignment
SELECT 
    u.id,
    u.email,
    u.full_name,
    STRING_AGG(r.name, ', ') as roles
FROM users u
LEFT JOIN user_roles ur ON u.id = ur.user_id
LEFT JOIN roles r ON ur.role_id = r.id
WHERE u.email = 'taii@gmail.com'
GROUP BY u.id, u.email, u.full_name;

-- Expected result: taii@gmail.com should have roles: USER, ADMIN
