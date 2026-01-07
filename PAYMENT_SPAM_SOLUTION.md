# Giải Pháp Cho Vấn Đề "Cứ Click Là Tạo Payment Mới"

## 🎯 Vấn Đề

Bạn test thấy:
- Mỗi lần click "Mua ngay" → tạo 1 payment record mới
- Database có rất nhiều PENDING payments
- PayOS dashboard cũng có rất nhiều payment links chưa thanh toán

## ✅ Giải Pháp Đã Implement

### 1. Payment Link Reuse (Mặc định - Production Ready)

**Cách hoạt động**:
- Nếu đã có pending payment cùng gói trong 10 phút → trả về link cũ
- Không tạo payment mới → giảm spam

**Config**:
```properties
payment.spam-block-minutes=10
payment.test-mode=false  # Mặc định
```

**Kết quả**:
```
Click lần 1: Tạo payment #123
Click lần 2: Trả về payment #123 (reuse)
Click lần 3: Trả về payment #123 (reuse)
→ Chỉ có 1 payment record
```

### 2. Test Mode (Chỉ Cho Development)

**Khi nào dùng**: Khi bạn đang test và muốn tạo nhiều payments

**Config**:
```properties
payment.test-mode=true  # BẬT test mode
```

**Kết quả**:
```
Click lần 1: Tạo payment #123
Click lần 2: Tạo payment #124
Click lần 3: Tạo payment #125
→ Mỗi lần click tạo payment mới
```

**⚠️ QUAN TRỌNG**: 
- CHỈ dùng `test-mode=true` khi development
- PHẢI tắt (`test-mode=false`) khi deploy production

## 📋 Hướng Dẫn Sử Dụng

### Scenario 1: Đang Development/Testing

**Muốn test nhiều lần**:
```properties
# application.properties
payment.test-mode=true
```

Hoặc dùng environment variable:
```bash
# Windows
set PAYMENT_TEST_MODE=true
.\mvnw.cmd spring-boot:run

# Linux/Mac
export PAYMENT_TEST_MODE=true
./mvnw spring-boot:run
```

### Scenario 2: Deploy Production

**PHẢI tắt test mode**:
```properties
# application.properties
payment.test-mode=false
```

Hoặc đảm bảo không set environment variable `PAYMENT_TEST_MODE`

### Scenario 3: Test Xong, Dọn Dẹp Database

Nếu test tạo quá nhiều PENDING payments, chạy SQL:

```sql
-- Xóa tất cả PENDING payments cũ hơn 1 giờ
DELETE FROM payments 
WHERE status = 'PENDING' 
AND created_at < DATEADD(HOUR, -1, GETDATE());

-- Hoặc đánh dấu EXPIRED thay vì xóa
UPDATE payments 
SET status = 'EXPIRED' 
WHERE status = 'PENDING' 
AND created_at < DATEADD(HOUR, -1, GETDATE());
```

## 🔧 Cleanup Task

Cleanup task tự động chạy mỗi 15 phút:
- Tìm PENDING payments > 30 phút
- Check status trên PayOS
- Đánh dấu EXPIRED/CANCELLED
- Giới hạn 20 payments/lần để tránh rate limit

**Config**:
```properties
payment.cleanup-interval-minutes=15
payment.stale-payment-minutes=30
payment.max-cleanup-batch-size=20
```

## 📊 So Sánh

### Trước Khi Fix
```
Test 10 lần:
- Database: 10 PENDING payments
- PayOS: 10 payment links
- User: Bị báo lỗi "Đã có giao dịch đang chờ"
```

### Sau Khi Fix (Reuse Mode)
```
Test 10 lần (trong 10 phút):
- Database: 1 PENDING payment
- PayOS: 1 payment link
- User: Nhận cùng 1 link, không bị lỗi
```

### Sau Khi Fix (Test Mode)
```
Test 10 lần:
- Database: 10 PENDING payments (OK cho testing)
- PayOS: 10 payment links
- User: Mỗi lần nhận link mới
- Cleanup task sẽ dọn sau 30 phút
```

## 💡 Khuyến Nghị

### Cho Development
```properties
# Khi test payment flow nhiều lần
payment.test-mode=true
payment.spam-block-minutes=5  # Ngắn hơn
```

### Cho Production
```properties
# PHẢI dùng config này
payment.test-mode=false
payment.spam-block-minutes=10  # Hoặc 15
```

## 🚀 Kết Luận

**Giải pháp tốt nhất**:
1. **Production**: Dùng reuse mode (`test-mode=false`)
2. **Development**: Bật test mode khi cần (`test-mode=true`)
3. **Cleanup**: Để cleanup task tự động dọn dẹp

**Lợi ích**:
✅ Giảm spam trong database
✅ Giảm số lượng PayOS API calls
✅ User experience tốt hơn
✅ Dễ quản lý và debug
✅ Linh hoạt cho cả dev và prod

---

**File liên quan**:
- `PAYMENT_REUSE_STRATEGY.md` - Chi tiết về reuse strategy
- `PAYMENT_SYSTEM_IMPROVEMENTS.md` - Tổng hợp các improvements
- `application.properties` - Config file
