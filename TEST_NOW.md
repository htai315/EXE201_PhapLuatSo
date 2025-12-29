# 🚀 Test Ngay - VNPay Hoàn Chỉnh

## ✅ Đã Fix

1. **Return URL**: Sửa từ `/payment-result.html` → `/html/payment-result.html`
2. **Đã copy** sang `target/classes/application.properties`

## 🎯 Test IPN Cho Payment Vừa Rồi

### Bước 1: Test IPN Manual
Mở browser, paste URL này:

```
http://localhost:8080/api/payment/vnpay-ipn?vnp_Amount=24900000&vnp_BankCode=NCB&vnp_BankTranNo=VNP15373172&vnp_CardType=ATM&vnp_OrderInfo=Payment_STUDENT&vnp_PayDate=20251229115930&vnp_ResponseCode=00&vnp_TmnCode=NA128BPU&vnp_TransactionNo=15373172&vnp_TransactionStatus=00&vnp_TxnRef=PAY1766984346014c2c66535&vnp_SecureHash=53289a82b0da44fd9dae255fdfe2861a4b95d63887a028fd5e4bbe7738ddd415cf2d6b88fd7c39bd2b78ca40f9560dfc4d35e59354825594e376695cdecc2352
```

### Bước 2: Xem Console Logs
Phải thấy:
```
VNPay IPN received: ...
Payment SUCCESS: txnRef=PAY1766984346014c2c66535
Credits added: user=2, chat=100, quiz=20
```

### Bước 3: Kiểm Tra Profile
Vào: `http://localhost:8080/profile.html`

Phải thấy:
- **Chat Credits**: 100
- **Quiz Gen Credits**: 20

## 🔄 Test Payment Mới (Với Return URL Đúng)

### Bước 1: RESTART Application
**QUAN TRỌNG**: Phải restart để load Return URL mới!

### Bước 2: Test Payment Flow
1. Vào: `http://localhost:8080/plans.html`
2. Login
3. Chọn plan bất kỳ
4. Thanh toán với thẻ test
5. **Lần này phải redirect về `/html/payment-result.html` ĐÚNG!**

### Bước 3: Xem Kết Quả
- ✅ Phải thấy trang "Thanh Toán Thành Công"
- ✅ Phải thấy thông tin credits
- ✅ Countdown 5 giây về trang chủ

## 📊 Tóm Tắt

| Vấn Đề | Giải Pháp | Trạng Thái |
|--------|-----------|-----------|
| Return URL sai | Sửa thành `/html/payment-result.html` | ✅ Fixed |
| File không tìm thấy | Return URL đúng path | ✅ Fixed |
| IPN chưa chạy | Test manual | ⏳ Cần test |
| Credits chưa cộng | Sau khi IPN chạy | ⏳ Cần test |

---

**Làm theo thứ tự:**
1. Test IPN manual (URL ở trên) → Cộng credits cho payment cũ
2. RESTART application → Load Return URL mới
3. Test payment mới → Verify Return URL đúng

🚀 **Bắt đầu ngay!**
