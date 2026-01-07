# Payment Link Reuse Strategy

## 🎯 Vấn Đề

Khi test, mỗi lần click "Mua ngay" tạo 1 payment record mới:
- Database của bạn: nhiều PENDING payments
- PayOS dashboard: nhiều payment links chưa thanh toán
- Gây lãng phí và khó quản lý

## ✅ Giải Pháp: REUSE Payment Link

### Cách Hoạt Động

**Trước đây**:
```
User click "Mua ngay" → Tạo payment mới → Báo lỗi "Đã có giao dịch đang chờ"
```

**Bây giờ**:
```
User click "Mua ngay" 
  → Kiểm tra có pending payment cùng gói không?
    → CÓ: Trả về link cũ (reuse)
    → KHÔNG: Tạo payment mới
```

### Code Logic

```java
if (latestPending.getPlan().getCode().equals(planCode) 
    && latestPending.getCreatedAt().isAfter(LocalDateTime.now().minusMinutes(10))) {
    
    // Lấy payment link cũ từ PayOS
    var existingPaymentInfo = payOS.paymentRequests().get(latestPending.getOrderCode());
    
    // Trả về link cũ
    return new CreatePaymentResponse(
        existingPaymentInfo.getCheckoutUrl(),
        String.valueOf(latestPending.getOrderCode()),
        existingPaymentInfo.getQrCode(),
        ...
    );
}
```

### Ví Dụ Thực Tế

**Scenario 1: User click nhiều lần trong 10 phút**
```
09:00 - Click "Mua gói BASIC" → Tạo payment #123
09:02 - Click "Mua gói BASIC" lại → Trả về payment #123 (reuse)
09:05 - Click "Mua gói BASIC" lại → Trả về payment #123 (reuse)
```
→ Chỉ có 1 payment record, 1 PayOS link

**Scenario 2: User đổi ý mua gói khác**
```
09:00 - Click "Mua gói BASIC" → Tạo payment #123
09:02 - Click "Mua gói PRO" → Tạo payment #124 (gói khác)
```
→ Cho phép tạo mới vì khác gói

**Scenario 3: Payment link expired**
```
09:00 - Click "Mua gói BASIC" → Tạo payment #123
09:20 - Click "Mua gói BASIC" lại → Payment #123 đã quá 10 phút
      → Tạo payment #124 mới
```

**Scenario 4: PayOS link không tồn tại**
```
09:00 - Click "Mua gói BASIC" → Tạo payment #123
09:02 - PayOS link #123 bị cancel/expire
09:03 - Click "Mua gói BASIC" lại → Không tìm thấy link trên PayOS
      → Đánh dấu #123 = EXPIRED
      → Tạo payment #124 mới
```

## 📊 So Sánh

### Trước Khi Fix
```
Test 10 lần → 10 payment records → 10 PayOS links
```

### Sau Khi Fix
```
Test 10 lần (trong 10 phút) → 1 payment record → 1 PayOS link
```

## ⚙️ Configuration

Trong `application.properties`:
```properties
# Thời gian reuse payment link (phút)
payment.spam-block-minutes=10

# Bật/tắt tính năng reuse (mặc định: true)
payment.reuse-pending-payment=true

# Test mode: tắt spam protection (chỉ dùng cho development)
payment.test-mode=false
```

### Chế Độ Test (Development Only)

Nếu bạn đang test và muốn tạo nhiều payments:

```properties
# Bật test mode - CHỈ DÙNG KHI TEST!
payment.test-mode=true
```

**⚠️ CẢNH BÁO**: 
- `test-mode=true` sẽ TẮT spam protection
- Cho phép tạo payment mới mỗi lần click
- **KHÔNG BAO GIỜ** dùng trong production!

### Cách Sử Dụng

**Khi Development/Testing**:
```properties
payment.test-mode=true  # Tạo payment mới mỗi lần
```

**Khi Production**:
```properties
payment.test-mode=false  # Reuse payment link (an toàn)
```

## 🎯 Lợi Ích

✅ **Giảm database records**: Ít PENDING payments hơn
✅ **Giảm PayOS API calls**: Không tạo link mới liên tục
✅ **Tốt cho PayOS dashboard**: Ít payment links rác
✅ **User experience tốt**: Cùng 1 link, không bị confuse
✅ **Dễ quản lý**: Admin dễ track payments

## 🔧 Cleanup Task

Cleanup task vẫn chạy mỗi 15 phút để:
- Đánh dấu EXPIRED cho payments > 30 phút
- Sync status từ PayOS
- Giới hạn 20 payments/lần để tránh rate limit

## 💡 Best Practices

### Cho Development/Testing
```properties
payment.spam-block-minutes=5  # Ngắn hơn để test nhanh
```

### Cho Production
```properties
payment.spam-block-minutes=15  # Dài hơn để giảm spam
```

## 🚀 Kết Luận

Với giải pháp này:
- **Development**: Test thoải mái, không tạo quá nhiều records
- **Production**: User có thể click lại "Mua ngay" mà không bị lỗi
- **PayOS Dashboard**: Sạch sẽ, dễ quản lý

**Recommendation**: Giữ `payment.spam-block-minutes=10` cho cả dev và prod! 👍
