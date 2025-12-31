# ⚡ Quick Start - Tính Năng Quên Mật Khẩu

## 🚀 Bắt Đầu Nhanh (5 phút)

### Bước 1: Cấu Hình Email (2 phút)

1. **Bật 2-Step Verification:**
   - Truy cập: https://myaccount.google.com/security
   - Bật "2-Step Verification"

2. **Tạo App Password:**
   - Truy cập: https://myaccount.google.com/apppasswords
   - Tạo password cho "Mail" → "Other (Pháp Luật Số)"
   - Sao chép mật khẩu 16 ký tự

3. **Cập nhật file `.env`:**
   ```env
   MAIL_HOST=smtp.gmail.com
   MAIL_PORT=587
   MAIL_USERNAME=your-email@gmail.com
   MAIL_PASSWORD=xxxx xxxx xxxx xxxx
   ```

### Bước 2: Chạy Ứng Dụng (1 phút)

```bash
mvn spring-boot:run
```

### Bước 3: Test (2 phút)

1. Mở trình duyệt: http://localhost:8080/html/login.html
2. Nhấn **"Quên mật khẩu?"**
3. Nhập email đã đăng ký
4. Kiểm tra email → Lấy mã OTP
5. Nhập OTP + mật khẩu mới
6. Đăng nhập với mật khẩu mới

---

## ✅ Xong!

Tính năng đã hoạt động! 🎉

---

## 🐛 Gặp Lỗi?

### Lỗi: "Authentication failed"
→ Kiểm tra lại App Password trong file `.env`

### Lỗi: "Email không tồn tại"
→ Đăng ký tài khoản trước khi test

### Lỗi: "OTP không hợp lệ"
→ Kiểm tra email mới nhất, OTP chỉ có hiệu lực 15 phút

---

## 📖 Đọc Thêm

- **Chi tiết:** `EMAIL_SETUP_GUIDE.md`
- **Tổng quan:** `PASSWORD_RESET_FEATURE.md`
- **API:** `API_DOCUMENTATION.md`

---

**Happy Coding!** 🚀
