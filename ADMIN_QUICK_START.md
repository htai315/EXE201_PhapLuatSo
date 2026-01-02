# 🚀 ADMIN SYSTEM - QUICK START GUIDE

## Bước 1: Kiểm tra hệ thống

Chạy script kiểm tra:
```sql
-- Chạy file test_admin_system.sql
```

Script này sẽ kiểm tra:
- ✅ Roles (USER, ADMIN) đã tồn tại chưa
- ✅ Admin user đã được tạo chưa
- ✅ Database fields và tables đã đầy đủ chưa
- ✅ Indexes đã được tạo chưa

## Bước 2: Setup Admin User

### Cách 1: Dùng script có sẵn

Chọn một trong các script sau:

**A. Tạo admin mới:**
```sql
-- Chạy file: create_new_admin.sql
-- Email: admin@phapluatso.vn
-- Password: Admin@123
```

**B. Promote user hiện có:**
```sql
-- Chạy file: promote_taii_to_admin.sql
-- Promote user "taii" thành admin
```

**C. Setup admin account (SQL Server):**
```sql
-- Chạy file: setup_admin_account_sqlserver.sql
```

### Cách 2: Tạo thủ công

```sql
-- 1. Tạo roles (nếu chưa có)
INSERT INTO roles (name) VALUES ('USER');
INSERT INTO roles (name) VALUES ('ADMIN');

-- 2. Tạo admin user
-- Password: Admin@123
-- Hash: $2a$10$YourBcryptHashHere
INSERT INTO users (email, password_hash, full_name, provider, email_verified, is_enabled, is_active)
VALUES ('admin@phapluatso.vn', '$2a$10$...', 'System Admin', 'LOCAL', 1, 1, 1);

-- 3. Gán role ADMIN
DECLARE @userId BIGINT = (SELECT id FROM users WHERE email = 'admin@phapluatso.vn');
DECLARE @adminRoleId BIGINT = (SELECT id FROM roles WHERE name = 'ADMIN');

INSERT INTO user_roles (user_id, role_id)
VALUES (@userId, @adminRoleId);

-- 4. Tạo credits (optional)
INSERT INTO user_credits (user_id, chat_credits, quiz_gen_credits)
VALUES (@userId, 999999, 999999);
```

## Bước 3: Test Login

### Test qua API:

```bash
# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@phapluatso.vn",
    "password": "Admin@123"
  }'

# Response:
{
  "accessToken": "eyJhbGc...",
  "refreshToken": "abc123...",
  "expiresIn": 3600
}
```

### Test qua Browser:

1. Mở: `http://localhost:8080/html/login.html`
2. Login với:
   - Email: `admin@phapluatso.vn`
   - Password: `Admin@123`
3. Sau khi login, vào: `http://localhost:8080/html/admin/dashboard.html`

## Bước 4: Kiểm tra Admin Dashboard

Sau khi login thành công, bạn sẽ thấy:

### Dashboard Page:
- 📊 Statistics cards (users, revenue, activities)
- 📈 Charts (revenue, user growth)
- 🔄 Real-time data

### Users Management:
- 👥 Danh sách users với pagination
- 🔍 Search users
- 🚫 Ban/Unban users
- 🗑️ Delete users
- 👁️ Xem chi tiết user

### Payments Management:
- 💳 Danh sách payments
- 📊 Payment statistics
- 💰 Revenue tracking

### Activity Logs:
- 📝 Lịch sử hoạt động admin
- 🔍 Audit trail
- ⏰ Timestamp tracking

## Bước 5: Test Admin Functions

### Test Ban User:

```bash
# Get user list
curl -X GET http://localhost:8080/api/admin/users \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN"

# Ban user
curl -X POST http://localhost:8080/api/admin/users/2/ban \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "reason": "Vi phạm điều khoản"
  }'

# Unban user
curl -X POST http://localhost:8080/api/admin/users/2/unban \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN"
```

### Test View Statistics:

```bash
# Dashboard stats
curl -X GET http://localhost:8080/api/admin/stats \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN"

# Revenue chart
curl -X GET "http://localhost:8080/api/admin/stats/revenue?from=2025-01-01&to=2025-01-31" \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN"

# User growth chart
curl -X GET "http://localhost:8080/api/admin/stats/user-growth?from=2025-01-01&to=2025-01-31" \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN"
```

## Troubleshooting

### Lỗi: "403 Forbidden"

**Nguyên nhân:** User không có role ADMIN

**Giải pháp:**
```sql
-- Kiểm tra roles của user
SELECT u.email, r.name 
FROM users u
JOIN user_roles ur ON u.id = ur.user_id
JOIN roles r ON ur.role_id = r.id
WHERE u.email = 'admin@phapluatso.vn';

-- Nếu không có ADMIN role, thêm vào:
DECLARE @userId BIGINT = (SELECT id FROM users WHERE email = 'admin@phapluatso.vn');
DECLARE @adminRoleId BIGINT = (SELECT id FROM roles WHERE name = 'ADMIN');

INSERT INTO user_roles (user_id, role_id)
VALUES (@userId, @adminRoleId);
```

### Lỗi: "USER role not found"

**Giải pháp:**
```sql
INSERT INTO roles (name) VALUES ('USER');
INSERT INTO roles (name) VALUES ('ADMIN');
```

### Lỗi: "Invalid credentials"

**Nguyên nhân:** Password hash không đúng

**Giải pháp:**
1. Dùng BCrypt để hash password mới
2. Update vào database:
```sql
UPDATE users 
SET password_hash = '$2a$10$NewHashHere'
WHERE email = 'admin@phapluatso.vn';
```

### Charts không hiển thị

**Giải pháp:**
1. Kiểm tra console browser (F12)
2. Kiểm tra Chart.js CDN có load được không
3. Kiểm tra API có trả về data không

## Security Checklist

Trước khi deploy production:

- [ ] Đổi password admin từ mặc định
- [ ] Set JWT secret key mạnh (>32 ký tự)
- [ ] Enable HTTPS
- [ ] Config CORS đúng
- [ ] Không commit secrets vào Git
- [ ] Set up environment variables
- [ ] Enable rate limiting
- [ ] Set up monitoring/logging

## Kết luận

Sau khi hoàn thành các bước trên, hệ thống admin đã sẵn sàng sử dụng! 🎉

**Các trang admin:**
- Dashboard: `/html/admin/dashboard.html`
- Users: `/html/admin/users.html`
- Payments: `/html/admin/payments.html`
- Activity Logs: `/html/admin/activity-logs.html`

**Default admin credentials:**
- Email: `admin@phapluatso.vn`
- Password: `Admin@123` (nhớ đổi sau khi login lần đầu!)

---

**Cần hỗ trợ?** Xem file `ADMIN_AUTH_REVIEW.md` để biết chi tiết đầy đủ về hệ thống.
