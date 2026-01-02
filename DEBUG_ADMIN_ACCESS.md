# 🔍 DEBUG: Không vào được trang Admin

## Bước 1: Kiểm tra Console Browser

1. Mở trang admin: `http://localhost:8080/html/admin/dashboard.html`
2. Nhấn F12 để mở Developer Tools
3. Vào tab **Console**
4. Xem có lỗi gì không?

### Các lỗi thường gặp:

**A. Lỗi 403 Forbidden:**
```
GET http://localhost:8080/api/admin/stats 403 (Forbidden)
```
→ User không có quyền ADMIN

**B. Lỗi 401 Unauthorized:**
```
GET http://localhost:8080/api/admin/stats 401 (Unauthorized)
```
→ Token không hợp lệ hoặc đã hết hạn

**C. Lỗi CORS:**
```
Access to fetch at '...' has been blocked by CORS policy
```
→ Vấn đề CORS configuration

**D. Lỗi Network:**
```
Failed to fetch
```
→ Backend không chạy hoặc URL sai

---

## Bước 2: Kiểm tra Token và Role

Mở Console (F12) và chạy các lệnh sau:

### A. Kiểm tra có token không:
```javascript
console.log('Token:', localStorage.getItem('token'));
```

**Kết quả mong đợi:** Phải có token (chuỗi dài)
**Nếu null:** Bạn chưa login hoặc token đã bị xóa

### B. Kiểm tra user info:
```javascript
fetch('/api/auth/me', {
    headers: {
        'Authorization': 'Bearer ' + localStorage.getItem('token')
    }
})
.then(r => r.json())
.then(user => console.log('User:', user))
.catch(err => console.error('Error:', err));
```

**Kết quả mong đợi:**
```json
{
  "id": 1,
  "email": "admin@example.com",
  "fullName": "Admin",
  "role": "ADMIN",  ← PHẢI LÀ "ADMIN"
  "avatarUrl": null
}
```

**Nếu role không phải "ADMIN":** Đây là vấn đề!

### C. Test API admin trực tiếp:
```javascript
fetch('/api/admin/stats', {
    headers: {
        'Authorization': 'Bearer ' + localStorage.getItem('token')
    }
})
.then(r => {
    console.log('Status:', r.status);
    return r.json();
})
.then(data => console.log('Data:', data))
.catch(err => console.error('Error:', err));
```

**Kết quả:**
- Status 200: OK, API hoạt động
- Status 403: Không có quyền ADMIN
- Status 401: Token không hợp lệ
- Error: Backend không chạy

---

## Bước 3: Kiểm tra Database

Chạy SQL sau để kiểm tra user có role ADMIN không:

```sql
-- Kiểm tra user và role
SELECT 
    u.id,
    u.email,
    u.full_name,
    u.is_enabled,
    u.is_active,
    STRING_AGG(r.name, ', ') AS roles
FROM users u
LEFT JOIN user_roles ur ON u.id = ur.user_id
LEFT JOIN roles r ON ur.role_id = r.id
WHERE u.email = 'YOUR_EMAIL_HERE'  -- Thay bằng email của bạn
GROUP BY u.id, u.email, u.full_name, u.is_enabled, u.is_active;
```

**Kết quả mong đợi:**
- `roles` phải có "ADMIN"
- `is_enabled` = 1
- `is_active` = 1

**Nếu không có role ADMIN, chạy:**
```sql
-- Thêm role ADMIN cho user
DECLARE @userId BIGINT = (SELECT id FROM users WHERE email = 'YOUR_EMAIL_HERE');
DECLARE @adminRoleId BIGINT = (SELECT id FROM roles WHERE name = 'ADMIN');

-- Kiểm tra role đã tồn tại chưa
IF NOT EXISTS (SELECT 1 FROM user_roles WHERE user_id = @userId AND role_id = @adminRoleId)
BEGIN
    INSERT INTO user_roles (user_id, role_id)
    VALUES (@userId, @adminRoleId);
    PRINT 'Role ADMIN đã được thêm!';
END
ELSE
BEGIN
    PRINT 'User đã có role ADMIN rồi!';
END
```

---

## Bước 4: Kiểm tra JWT Token

### A. Decode JWT token:

1. Copy token từ localStorage
2. Vào https://jwt.io
3. Paste token vào
4. Xem payload:

```json
{
  "sub": "admin@example.com",
  "uid": 1,
  "roles": ["ADMIN"],  ← PHẢI CÓ "ADMIN"
  "iat": 1234567890,
  "exp": 1234571490
}
```

**Kiểm tra:**
- `roles` phải chứa "ADMIN"
- `exp` (expiry) chưa hết hạn (timestamp > hiện tại)

### B. Nếu token hết hạn:

Logout và login lại để lấy token mới:
```javascript
// Logout
localStorage.removeItem('token');
localStorage.removeItem('refreshToken');

// Reload page và login lại
window.location.href = '/html/login.html';
```

---

## Bước 5: Kiểm tra Backend

### A. Backend có đang chạy không?

Test endpoint:
```bash
curl http://localhost:8080/api/auth/test
```

**Kết quả mong đợi:** "Auth controller is working"

### B. Test admin endpoint với token:

```bash
# Thay YOUR_TOKEN bằng token thật
curl -X GET http://localhost:8080/api/admin/stats \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Kết quả:**
- 200 + data: OK
- 403: Không có quyền
- 401: Token không hợp lệ

---

## Bước 6: Các giải pháp thường gặp

### Giải pháp 1: Thêm role ADMIN

```sql
-- Chạy script này
DECLARE @userId BIGINT = (SELECT id FROM users WHERE email = 'YOUR_EMAIL');
DECLARE @adminRoleId BIGINT = (SELECT id FROM roles WHERE name = 'ADMIN');

INSERT INTO user_roles (user_id, role_id)
VALUES (@userId, @adminRoleId);
```

### Giải pháp 2: Refresh token

```javascript
// Trong Console browser
localStorage.removeItem('token');
localStorage.removeItem('refreshToken');
window.location.href = '/html/login.html';
```

### Giải pháp 3: Kiểm tra SecurityConfig

Đảm bảo admin endpoints được config đúng:
```java
.requestMatchers("/api/admin/**").hasRole("ADMIN")
```

### Giải pháp 4: Clear cache và hard refresh

1. Nhấn `Ctrl + Shift + Delete`
2. Xóa cache và cookies
3. Hard refresh: `Ctrl + Shift + R`
4. Login lại

---

## Bước 7: Test script nhanh

Chạy script này trong Console để test toàn bộ:

```javascript
(async function testAdminAccess() {
    console.log('=== TESTING ADMIN ACCESS ===\n');
    
    // 1. Check token
    const token = localStorage.getItem('token');
    console.log('1. Token exists:', !!token);
    if (!token) {
        console.error('❌ No token found! Please login first.');
        return;
    }
    
    // 2. Check user info
    console.log('\n2. Checking user info...');
    try {
        const userRes = await fetch('/api/auth/me', {
            headers: { 'Authorization': 'Bearer ' + token }
        });
        
        if (!userRes.ok) {
            console.error('❌ Failed to get user info:', userRes.status);
            return;
        }
        
        const user = await userRes.json();
        console.log('✓ User:', user);
        console.log('✓ Role:', user.role);
        
        if (user.role !== 'ADMIN') {
            console.error('❌ User is not ADMIN! Current role:', user.role);
            console.log('\n💡 Solution: Run SQL to add ADMIN role');
            return;
        }
        
        console.log('✓ User has ADMIN role!');
        
    } catch (err) {
        console.error('❌ Error getting user info:', err);
        return;
    }
    
    // 3. Test admin API
    console.log('\n3. Testing admin API...');
    try {
        const adminRes = await fetch('/api/admin/stats', {
            headers: { 'Authorization': 'Bearer ' + token }
        });
        
        console.log('Status:', adminRes.status);
        
        if (adminRes.status === 403) {
            console.error('❌ 403 Forbidden - No admin permission');
            console.log('💡 Check database: user_roles table');
            return;
        }
        
        if (adminRes.status === 401) {
            console.error('❌ 401 Unauthorized - Token invalid');
            console.log('💡 Logout and login again');
            return;
        }
        
        if (!adminRes.ok) {
            console.error('❌ API error:', adminRes.status);
            return;
        }
        
        const stats = await adminRes.json();
        console.log('✓ Admin API works!');
        console.log('Stats:', stats);
        
        console.log('\n✅ ALL TESTS PASSED! You can access admin dashboard.');
        
    } catch (err) {
        console.error('❌ Error testing admin API:', err);
        console.log('💡 Check if backend is running');
    }
})();
```

---

## Kết quả mong đợi:

```
=== TESTING ADMIN ACCESS ===

1. Token exists: true

2. Checking user info...
✓ User: {id: 1, email: "admin@example.com", ...}
✓ Role: ADMIN
✓ User has ADMIN role!

3. Testing admin API...
Status: 200
✓ Admin API works!
Stats: {totalUsers: 10, ...}

✅ ALL TESTS PASSED! You can access admin dashboard.
```

---

## Nếu vẫn không được:

Gửi cho tôi kết quả của:
1. Console errors (screenshot)
2. Kết quả test script trên
3. Kết quả SQL query kiểm tra role

Tôi sẽ giúp bạn debug tiếp!
