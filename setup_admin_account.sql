-- Setup Admin Account for Pháp Luật Số
-- Run this script after V4 migration

-- Step 1: Create ADMIN role if not exists
INSERT INTO roles (name) 
VALUES ('ADMIN') 
ON CONFLICT (name) DO NOTHING;

-- Step 2: Create USER role if not exists (for regular users)
INSERT INTO roles (name) 
VALUES ('USER') 
ON CONFLICT (name) DO NOTHING;

-- Step 3: Find or create admin user
-- Option A: If you already have an account, find its ID
-- SELECT id, email FROM users WHERE email = 'your-email@example.com';

-- Option B: Create a new admin account (uncomment and modify)
-- Note: You'll need to register through the app first, then run this script
-- Or manually insert with hashed password

-- Step 4: Assign ADMIN role to user
-- Replace 'your-email@example.com' with your actual email
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id 
FROM users u, roles r 
WHERE u.email = 'your-email@example.com' 
  AND r.name = 'ADMIN'
ON CONFLICT DO NOTHING;

-- Step 5: Also assign USER role (users can have multiple roles)
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id 
FROM users u, roles r 
WHERE u.email = 'your-email@example.com' 
  AND r.name = 'USER'
ON CONFLICT DO NOTHING;

-- Step 6: Verify the setup
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
WHERE u.email = 'your-email@example.com'
GROUP BY u.id, u.email, u.full_name, u.is_active, u.is_enabled;
