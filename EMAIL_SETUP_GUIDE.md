# 📧 Hướng Dẫn Cấu Hình Email cho Tính Năng Quên Mật Khẩu

## 🎯 Tổng Quan

Tính năng "Quên mật khẩu" sử dụng Gmail SMTP để gửi mã OTP (6 số) đến email người dùng. OTP có hiệu lực trong 15 phút.

---

## 📋 Yêu Cầu

1. **Tài khoản Gmail** (hoặc Google Workspace)
2. **Bật xác thực 2 bước** (2-Step Verification)
3. **Tạo App Password** (mật khẩu ứng dụng)

---

## 🔧 Cách Cấu Hình

### Bước 1: Bật Xác Thực 2 Bước

1. Truy cập: https://myaccount.google.com/security
2. Tìm mục **"2-Step Verification"**
3. Nhấn **"Get Started"** và làm theo hướng dẫn
4. Xác thực bằng số điện thoại hoặc ứng dụng Authenticator

### Bước 2: Tạo App Password

1. Sau khi bật 2-Step Verification, truy cập: https://myaccount.google.com/apppasswords
2. Chọn **"Select app"** → **"Mail"**
3. Chọn **"Select device"** → **"Other (Custom name)"**
4. Nhập tên: `Pháp Luật Số`
5. Nhấn **"Generate"**
6. **Sao chép mật khẩu 16 ký tự** (dạng: `xxxx xxxx xxxx xxxx`)

### Bước 3: Cập Nhật File .env

Mở file `.env` và thêm/cập nhật các dòng sau:

```env
# Email Configuration (Gmail SMTP)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=xxxx xxxx xxxx xxxx
```

**Lưu ý:**
- `MAIL_USERNAME`: Email Gmail của bạn (ví dụ: `phapluatso@gmail.com`)
- `MAIL_PASSWORD`: App Password 16 ký tự vừa tạo (giữ nguyên khoảng trắng hoặc xóa hết khoảng trắng đều được)

### Bước 4: Khởi Động Lại Ứng Dụng

```bash
# Dừng ứng dụng (nếu đang chạy)
# Ctrl+C

# Khởi động lại
mvn spring-boot:run
```

---

## ✅ Kiểm Tra Cấu Hình

### Test 1: Gửi OTP

1. Truy cập: http://localhost:8080/html/forgot-password.html
2. Nhập email đã đăng ký
3. Nhấn **"Gửi Mã OTP"**
4. Kiểm tra hộp thư email

**Kết quả mong đợi:**
- ✅ Nhận được email với mã OTP 6 số
- ✅ Email có tiêu đề: "Mã OTP đặt lại mật khẩu - Pháp Luật Số"

### Test 2: Đặt Lại Mật Khẩu

1. Nhập mã OTP từ email
2. Nhập mật khẩu mới (tối thiểu 6 ký tự)
3. Xác nhận mật khẩu
4. Nhấn **"Đặt Lại Mật Khẩu"**

**Kết quả mong đợi:**
- ✅ Thông báo "Đặt lại mật khẩu thành công"
- ✅ Chuyển hướng về trang đăng nhập
- ✅ Đăng nhập được với mật khẩu mới

---

## 🐛 Xử Lý Lỗi

### Lỗi 1: "Authentication failed"

**Nguyên nhân:**
- Chưa bật 2-Step Verification
- App Password sai
- Email/password không đúng

**Giải pháp:**
1. Kiểm tra lại 2-Step Verification đã bật chưa
2. Tạo lại App Password mới
3. Kiểm tra file `.env` có đúng không

### Lỗi 2: "Connection timeout"

**Nguyên nhân:**
- Firewall chặn port 587
- Không có kết nối internet

**Giải pháp:**
1. Kiểm tra kết nối internet
2. Tắt firewall tạm thời để test
3. Thử đổi port sang 465 (SSL):
   ```env
   MAIL_PORT=465
   ```

### Lỗi 3: "Email không tồn tại trong hệ thống"

**Nguyên nhân:**
- Email chưa đăng ký tài khoản

**Giải pháp:**
1. Đăng ký tài khoản trước
2. Hoặc dùng email đã đăng ký

### Lỗi 4: "OTP không hợp lệ hoặc đã được sử dụng"

**Nguyên nhân:**
- OTP đã hết hạn (> 15 phút)
- OTP đã được sử dụng rồi
- Nhập sai OTP

**Giải pháp:**
1. Nhấn **"Gửi Lại Mã OTP"**
2. Kiểm tra email mới nhất
3. Nhập đúng 6 số

---

## 🔒 Bảo Mật

### ✅ Đã Làm

- ✅ OTP chỉ có hiệu lực 15 phút
- ✅ OTP chỉ dùng được 1 lần
- ✅ Mật khẩu được hash bằng BCrypt
- ✅ Tự động xóa OTP hết hạn (mỗi giờ)
- ✅ Validate email tồn tại trong hệ thống
- ✅ Không cho phép reset password cho tài khoản Google OAuth2

### ⚠️ Khuyến Nghị

- Không commit file `.env` lên Git
- Sử dụng email riêng cho production
- Giới hạn số lần gửi OTP (rate limiting) - TODO
- Log tất cả các lần reset password

---

## 📊 Database Schema

```sql
CREATE TABLE password_reset_otps (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    email NVARCHAR(255) NOT NULL,
    otp NVARCHAR(6) NOT NULL,
    expires_at DATETIME2 NOT NULL,
    is_used BIT NOT NULL DEFAULT 0,
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    
    INDEX idx_email (email),
    INDEX idx_otp (otp),
    INDEX idx_expires_at (expires_at)
);
```

---

## 🚀 API Endpoints

### 1. Gửi OTP

**POST** `/api/auth/password-reset/send-otp`

**Request:**
```json
{
  "email": "user@example.com"
}
```

**Response (Success):**
```json
{
  "message": "Mã OTP đã được gửi đến email của bạn. Vui lòng kiểm tra hộp thư."
}
```

**Response (Error):**
```json
{
  "error": "Email không tồn tại trong hệ thống"
}
```

### 2. Đặt Lại Mật Khẩu

**POST** `/api/auth/password-reset/reset`

**Request:**
```json
{
  "email": "user@example.com",
  "otp": "123456",
  "newPassword": "newpassword123"
}
```

**Response (Success):**
```json
{
  "message": "Đặt lại mật khẩu thành công. Bạn có thể đăng nhập với mật khẩu mới."
}
```

**Response (Error):**
```json
{
  "error": "OTP không hợp lệ hoặc đã được sử dụng"
}
```

---

## 📝 Nội Dung Email

```
Xin chào,

Bạn đã yêu cầu đặt lại mật khẩu cho tài khoản Pháp Luật Số.

Mã OTP của bạn là: 123456

Mã này có hiệu lực trong 15 phút.

Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.

Trân trọng,
Đội ngũ Pháp Luật Số
```

---

## 🎨 Frontend Pages

### 1. Forgot Password Page
- **URL:** `/html/forgot-password.html`
- **Chức năng:** Nhập email để nhận OTP

### 2. Reset Password Page
- **URL:** `/html/reset-password.html?email=xxx`
- **Chức năng:** Nhập OTP và mật khẩu mới

---

## 🧪 Testing

### Manual Testing

1. **Test gửi OTP:**
   ```bash
   curl -X POST http://localhost:8080/api/auth/password-reset/send-otp \
     -H "Content-Type: application/json" \
     -d '{"email":"test@example.com"}'
   ```

2. **Test reset password:**
   ```bash
   curl -X POST http://localhost:8080/api/auth/password-reset/reset \
     -H "Content-Type: application/json" \
     -d '{"email":"test@example.com","otp":"123456","newPassword":"newpass123"}'
   ```

### Automated Testing (TODO)

- Unit tests cho `PasswordResetService`
- Integration tests cho API endpoints
- E2E tests cho frontend flow

---

## 📈 Monitoring

### Logs

Kiểm tra logs để debug:

```bash
# Xem logs gửi email
tail -f logs/spring.log | grep "OTP"

# Xem logs cleanup
tail -f logs/spring.log | grep "cleanup"
```

### Metrics (TODO)

- Số lượng OTP được gửi
- Số lượng reset password thành công
- Số lượng OTP hết hạn
- Thời gian trung bình để reset password

---

## 🔄 Scheduled Tasks

### Cleanup Expired OTPs

**Cron:** `0 0 * * * *` (Chạy vào đầu mỗi giờ)

**Chức năng:** Tự động xóa các OTP đã hết hạn khỏi database

**Code:**
```java
@Scheduled(cron = "0 0 * * * *")
@Transactional
public void cleanupExpiredOtps() {
    otpRepo.deleteExpiredOtps(LocalDateTime.now());
    System.out.println("🧹 Đã xóa các OTP hết hạn");
}
```

---

## 🌐 Production Deployment

### Checklist

- [ ] Tạo email riêng cho production (ví dụ: `noreply@phapluatso.com`)
- [ ] Cập nhật `.env` với thông tin production
- [ ] Test gửi email trên production
- [ ] Setup monitoring và alerting
- [ ] Thêm rate limiting (giới hạn 3 lần/15 phút)
- [ ] Thêm CAPTCHA để chống spam
- [ ] Setup email templates đẹp hơn (HTML email)

---

## 💡 Cải Tiến Tương Lai

### Phase 1 (Ngắn hạn)
- [ ] HTML email template đẹp hơn
- [ ] Rate limiting (giới hạn số lần gửi OTP)
- [ ] CAPTCHA để chống spam
- [ ] Email verification khi đăng ký

### Phase 2 (Trung hạn)
- [ ] SMS OTP (ngoài email)
- [ ] Thông báo khi có người reset password
- [ ] Admin dashboard để xem logs reset password
- [ ] Multi-language support

### Phase 3 (Dài hạn)
- [ ] Passwordless authentication
- [ ] Biometric authentication
- [ ] Social recovery (reset qua bạn bè)

---

## 📞 Hỗ Trợ

Nếu gặp vấn đề, vui lòng:

1. Kiểm tra logs: `logs/spring.log`
2. Kiểm tra file `.env` đã đúng chưa
3. Test với email khác
4. Liên hệ team support

---

**Tác giả:** AI Assistant  
**Ngày:** 31/12/2024  
**Version:** 1.0.0

