# 🔐 ADMIN ACCOUNT SETUP GUIDE

Hướng dẫn tạo tài khoản admin cho Pháp Luật Số

---

## 📋 YÊU CẦU

- ✅ Application đã compile thành công
- ✅ Database đã chạy migration V4
- ✅ Application đang chạy

---

## 🚀 CÁCH 1: TẠO ADMIN TỪ TÀI KHOẢN HIỆN CÓ (KHUYẾN NGHỊ)

### Bước 1: Đăng ký tài khoản thông thường

1. Mở trình duyệt: `http://localhost:8080`
2. Click "Đăng ký" hoặc vào: `http://localhost:8080/html/register.html`
3. Điền thông tin:
   - Email: `admin@example.com` (hoặc email bạn muốn)
   - Password: `Admin@123` (hoặc password bạn muốn)
   - Full Name: `Admin User`
4. Click "Đăng ký"

### Bước 2: Kết nối database

Mở PostgreSQL client (pgAdmin, DBeaver, hoặc psql):

```bash
psql -U postgres -d phapluatso
```

### Bước 3: Tạo ADMIN role

```sql
-- Tạo ADMIN role
INSERT INTO roles (name) 
VALUES ('ADMIN') 
ON CONFLICT (name) DO NOTHING;

-- Tạo USER role (nếu chưa có)
INSERT INTO roles (name) 
VALUES ('USER') 
ON CONFLICT (name) DO NOTHING;
```

### Bước 4: Gán ADMIN role cho user

**Thay `admin@example.com` bằng email bạn đã đăng ký:**

```sql
-- Gán ADMIN role
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id 
FROM users u, roles r 
WHERE u.email = 'admin@example.com' 
  AND r.name = 'ADMIN'
ON CONFLICT DO NOTHING;

-- Gán USER role (để vẫn dùng được các tính năng user)
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id 
FROM users u, roles r 
WHERE u.email = 'admin@example.com' 
  AND r.name = 'USER'
ON CONFLICT DO NOTHING;
```

### Bước 5: Verify

```sql
-- Kiểm tra roles của user
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
WHERE u.email = 'admin@example.com'
GROUP BY u.id, u.email, u.full_name, u.is_active, u.is_enabled;
```

**Kết quả mong đợi:**
```
 id |       email        | full_name  | is_active | is_enabled |    roles    
----+--------------------+------------+-----------+------------+-------------
  1 | admin@example.com  | Admin User |     t     |     t      | USER, ADMIN
```

### Bước 6: Đăng nhập và test

1. Logout (nếu đang đăng nhập)
2. Login lại với tài khoản admin
3. Truy cập: `http://localhost:8080/html/admin/dashboard.html`
4. Nếu thành công → Bạn sẽ thấy Admin Dashboard! 🎉

---

## 🚀 CÁCH 2: SỬ DỤNG SQL SCRIPT

### Bước 1: Đăng ký tài khoản

Đăng ký tài khoản như Cách 1 - Bước 1

### Bước 2: Chạy SQL script

```bash
psql -U postgres -d phapluatso -f setup_admin_account.sql
```

**Lưu ý:** Sửa email trong file `setup_admin_account.sql` trước khi chạy!

---

## 🚀 CÁCH 3: TẠO ADMIN BẰNG CODE (CHO DEV)

Nếu bạn muốn tự động tạo admin khi start app, thêm code này:

### Tạo file: `src/main/java/com/htai/exe201phapluatso/config/AdminInitializer.java`

```java
package com.htai.exe201phapluatso.config;

import com.htai.exe201phapluatso.auth.entity.Role;
import com.htai.exe201phapluatso.auth.entity.User;
import com.htai.exe201phapluatso.auth.repo.RoleRepo;
import com.htai.exe201phapluatso.auth.repo.UserRepo;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

@Configuration
public class AdminInitializer {

    @Bean
    CommandLineRunner initAdmin(UserRepo userRepo, RoleRepo roleRepo, PasswordEncoder passwordEncoder) {
        return args -> {
            // Create roles if not exist
            Role userRole = roleRepo.findByName("USER")
                .orElseGet(() -> {
                    Role r = new Role();
                    r.setName("USER");
                    return roleRepo.save(r);
                });
                
            Role adminRole = roleRepo.findByName("ADMIN")
                .orElseGet(() -> {
                    Role r = new Role();
                    r.setName("ADMIN");
                    return roleRepo.save(r);
                });
            
            // Create admin user if not exist
            String adminEmail = "admin@phapluatso.com";
            if (!userRepo.existsByEmail(adminEmail)) {
                User admin = new User();
                admin.setEmail(adminEmail);
                admin.setPasswordHash(passwordEncoder.encode("Admin@123"));
                admin.setFullName("System Admin");
                admin.setProvider("LOCAL");
                admin.setEmailVerified(true);
                admin.setEnabled(true);
                admin.setActive(true);
                admin.setRoles(Set.of(userRole, adminRole));
                
                userRepo.save(admin);
                System.out.println("✅ Admin account created: " + adminEmail + " / Admin@123");
            }
        };
    }
}
```

**Sau đó restart app, admin account sẽ tự động được tạo!**

---

## 🔍 TROUBLESHOOTING

### Lỗi: "403 Forbidden" khi truy cập admin dashboard

**Nguyên nhân:** User chưa có ADMIN role

**Giải pháp:**
```sql
-- Kiểm tra roles của user
SELECT u.email, r.name 
FROM users u
LEFT JOIN user_roles ur ON u.id = ur.user_id
LEFT JOIN roles r ON ur.role_id = r.id
WHERE u.email = 'your-email@example.com';

-- Nếu không có ADMIN role, gán lại
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id 
FROM users u, roles r 
WHERE u.email = 'your-email@example.com' 
  AND r.name = 'ADMIN';
```

### Lỗi: "Redirect to login" khi truy cập admin dashboard

**Nguyên nhân:** Chưa đăng nhập hoặc token hết hạn

**Giải pháp:**
1. Logout
2. Login lại
3. Kiểm tra token trong localStorage (F12 → Application → Local Storage)

### Lỗi: "roles table does not exist"

**Nguyên nhân:** Chưa chạy migration V4

**Giải pháp:**
```bash
.\mvnw.cmd flyway:migrate
```

### Lỗi: "user_roles table does not exist"

**Nguyên nhân:** Chưa chạy migration V4

**Giải pháp:** Chạy migration V4 (xem file `V4__add_admin_features.sql`)

---

## 📝 THÔNG TIN ADMIN MẶC ĐỊNH

Nếu dùng Cách 3 (AdminInitializer):

```
Email: admin@phapluatso.com
Password: Admin@123
```

**⚠️ LƯU Ý:** Đổi password ngay sau khi đăng nhập lần đầu!

---

## ✅ KIỂM TRA ADMIN DASHBOARD

Sau khi setup xong, test các tính năng:

1. **Dashboard:** `http://localhost:8080/html/admin/dashboard.html`
   - Xem statistics
   - Xem charts

2. **User Management:** `http://localhost:8080/html/admin/users.html`
   - Xem danh sách users
   - Search users
   - Ban/Unban users

3. **Payment Management:** `http://localhost:8080/html/admin/payments.html`
   - Xem danh sách payments
   - Xem statistics

4. **Activity Logs:** `http://localhost:8080/html/admin/activity-logs.html`
   - Xem lịch sử hành động admin

---

## 🎯 NEXT STEPS

Sau khi có admin account:

1. ✅ Test tất cả tính năng admin
2. ✅ Tạo thêm admin accounts nếu cần
3. ✅ Setup monitoring và logging
4. ✅ Deploy to production

---

**Chúc bạn setup thành công! 🎉**
