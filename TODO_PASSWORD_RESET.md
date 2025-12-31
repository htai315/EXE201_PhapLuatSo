# 📝 TODO: Cấu Hình Tính Năng Quên Mật Khẩu

**Trạng thái:** ⏸️ TẠM HOÃN (Chờ cấu hình email)

---

## ✅ Đã Làm

- ✅ Code backend hoàn chỉnh
- ✅ Code frontend hoàn chỉnh
- ✅ Database migration
- ✅ Documentation đầy đủ
- ✅ Tính năng có thể chạy mà không cần email (log OTP ra console)

---

## ⏳ Chưa Làm

- ⏸️ Cấu hình email Gmail SMTP
- ⏸️ Test gửi email thật

---

## 🚀 Khi Nào Muốn Bật Tính Năng

### Bước 1: Tạo App Password từ Gmail
1. Truy cập: https://myaccount.google.com/security
2. Bật "2-Step Verification"
3. Truy cập: https://myaccount.google.com/apppasswords
4. Tạo password cho "Mail" → "Other (Pháp Luật Số)"
5. Copy mật khẩu 16 ký tự

### Bước 2: Cập nhật file `.env`
```env
MAIL_ENABLED=true
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=xxxx xxxx xxxx xxxx
```

### Bước 3: Restart app
```bash
mvn spring-boot:run
```

### Bước 4: Test
- Vào: http://localhost:8080/html/login.html
- Nhấn "Quên mật khẩu?"
- Kiểm tra email

---

## 🧪 Test Hiện Tại (Không Cần Email)

Hiện tại tính năng vẫn hoạt động, nhưng OTP sẽ được **log ra console** thay vì gửi email:

1. Vào: http://localhost:8080/html/forgot-password.html
2. Nhập email đã đăng ký
3. Nhấn "Gửi Mã OTP"
4. **Xem console/logs** để lấy OTP
5. Nhập OTP vào trang reset-password.html
6. Đặt lại mật khẩu thành công

**Console sẽ hiển thị:**
```
⚠️ Email chưa được cấu hình. OTP cho user@example.com là: 123456
📧 Nội dung email:
Xin chào,

Bạn đã yêu cầu đặt lại mật khẩu cho tài khoản Pháp Luật Số.

Mã OTP của bạn là: 123456

Mã này có hiệu lực trong 15 phút.
...
---
```

---

## 📖 Tài Liệu

- **Quick Start:** `QUICK_START_PASSWORD_RESET.md`
- **Chi tiết:** `EMAIL_SETUP_GUIDE.md`
- **Tổng quan:** `PASSWORD_RESET_FEATURE.md`
- **Tóm tắt:** `PASSWORD_RESET_SUMMARY.md`

---

## 💡 Lưu Ý

- Tính năng đã sẵn sàng, chỉ cần cấu hình email
- Không ảnh hưởng đến các tính năng khác
- Có thể test được mà không cần email (xem console)
- Khi nào cần thì bật lên, rất đơn giản

---

**Nhớ làm sau khi có email nhé!** 📧
