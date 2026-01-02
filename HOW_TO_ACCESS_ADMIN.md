# 🎯 CÁCH VÀO TRANG ADMIN

## ✅ Bạn đã có admin account rồi!

Có **3 cách** để vào trang admin:

---

## 🚀 Cách 1: Truy cập trực tiếp (Nhanh nhất)

Sau khi login với admin account, mở browser và truy cập:

```
http://localhost:8080/html/admin/dashboard.html
```

### Các trang admin khác:

| Trang | URL |
|-------|-----|
| 📊 Dashboard | `/html/admin/dashboard.html` |
| 👥 Quản lý Users | `/html/admin/users.html` |
| 💳 Quản lý Payments | `/html/admin/payments.html` |
| 📝 Activity Logs | `/html/admin/activity-logs.html` |

---

## 🔗 Cách 2: Qua navbar (Đã tự động thêm)

Tôi đã thêm script tự động hiển thị link "Admin Panel" trong navbar!

**Cách hoạt động:**
1. Login với admin account
2. Nhìn lên góc phải navbar
3. Click vào avatar của bạn
4. Sẽ thấy menu dropdown với link **"🛡️ Admin Panel"** màu đỏ
5. Click vào là vào được admin dashboard!

**Script đã thêm:** `admin-nav-link.js`
- Tự động check role của user
- Nếu là ADMIN → hiển thị link
- Nếu không phải ADMIN → không hiển thị gì

---

## 📱 Cách 3: Bookmark (Tiện lợi)

Thêm bookmark vào browser:

1. Vào trang admin dashboard
2. Nhấn `Ctrl + D` (Windows) hoặc `Cmd + D` (Mac)
3. Lưu bookmark với tên "Admin Panel"
4. Lần sau chỉ cần click bookmark là vào được!

---

## 🔐 Kiểm tra quyền Admin

Để chắc chắn account của bạn có quyền admin:

### Cách 1: Qua API
```bash
# Login và lấy token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "your-admin-email@example.com",
    "password": "your-password"
  }'

# Check user info
curl -X GET http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer YOUR_TOKEN"

# Response sẽ có:
{
  "id": 1,
  "email": "admin@example.com",
  "fullName": "Admin",
  "role": "ADMIN",  <-- Phải là "ADMIN"
  ...
}
```

### Cách 2: Qua Database
```sql
-- Kiểm tra user có role ADMIN không
SELECT u.email, r.name AS role
FROM users u
JOIN user_roles ur ON u.id = ur.user_id
JOIN roles r ON ur.role_id = r.id
WHERE u.email = 'your-admin-email@example.com';

-- Kết quả phải có row với role = 'ADMIN'
```

---

## ⚠️ Troubleshooting

### Lỗi: "403 Forbidden" khi vào admin page

**Nguyên nhân:** User không có role ADMIN

**Giải pháp:**
```sql
-- Thêm role ADMIN cho user
DECLARE @userId BIGINT = (SELECT id FROM users WHERE email = 'your-email@example.com');
DECLARE @adminRoleId BIGINT = (SELECT id FROM roles WHERE name = 'ADMIN');

INSERT INTO user_roles (user_id, role_id)
VALUES (@userId, @adminRoleId);
```

### Link "Admin Panel" không hiển thị trong navbar

**Nguyên nhân:**
- Script chưa load
- User chưa login
- User không có role ADMIN

**Giải pháp:**
1. Mở Console (F12) → xem có lỗi không
2. Kiểm tra `localStorage.getItem('token')` có giá trị không
3. Kiểm tra role trong `/api/auth/me`
4. Hard refresh: `Ctrl + Shift + R`

### Trang admin hiển thị nhưng không có data

**Nguyên nhân:** Backend chưa chạy hoặc API lỗi

**Giải pháp:**
1. Kiểm tra backend đang chạy: `http://localhost:8080/api/admin/stats`
2. Xem Console (F12) → Network tab
3. Kiểm tra có lỗi 401/403/500 không

---

## 🎨 Giao diện Admin Dashboard

Khi vào admin dashboard, bạn sẽ thấy:

### 📊 Dashboard (Trang chính)
- **Statistics Cards:**
  - Tổng users, users active, users mới, users bị ban
  - Tổng doanh thu, doanh thu 30 ngày, tổng giao dịch
  - Quiz sets, quiz attempts, chat sessions, chat messages

- **Charts:**
  - Biểu đồ doanh thu 30 ngày (line chart)
  - Biểu đồ tăng trưởng users (line chart)

### 👥 Users Management
- Danh sách tất cả users với pagination
- Search users theo email/tên
- Xem chi tiết user (credits, payments, activities)
- Ban/Unban users
- Delete users (soft delete)

### 💳 Payments Management
- Danh sách tất cả payments
- Filter theo status (SUCCESS, FAILED, PENDING)
- Xem thống kê payments
- Sort theo date, amount

### 📝 Activity Logs
- Lịch sử tất cả actions của admin
- Xem ai đã ban/unban/delete user nào
- Timestamp và description đầy đủ

---

## 🎯 Quick Actions

Một số actions thường dùng:

### Ban một user:
1. Vào Users Management
2. Tìm user cần ban
3. Click nút "Ban"
4. Nhập lý do ban
5. Confirm

### Xem thống kê doanh thu:
1. Vào Dashboard
2. Xem statistics cards
3. Xem revenue chart
4. Hoặc vào Payments → View Stats

### Xem lịch sử hoạt động:
1. Vào Activity Logs
2. Xem danh sách actions
3. Filter theo date nếu cần

---

## 🚀 Bắt đầu ngay!

**Bước 1:** Login với admin account

**Bước 2:** Vào một trong các URL sau:
- `http://localhost:8080/html/admin/dashboard.html`
- Hoặc click "Admin Panel" trong navbar dropdown

**Bước 3:** Enjoy! 🎉

---

## 📞 Cần hỗ trợ?

Nếu gặp vấn đề, check các file sau:
- `ADMIN_AUTH_REVIEW.md` - Chi tiết đầy đủ về hệ thống
- `ADMIN_QUICK_START.md` - Hướng dẫn setup
- `test_admin_system.sql` - Script kiểm tra database

Hoặc mở Console (F12) để xem lỗi chi tiết!
