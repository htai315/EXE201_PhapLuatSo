# VNPay Sandbox Implementation - Checklist

## 🎯 Mục tiêu: Test thanh toán miễn phí với VNPay Sandbox

---

## ✅ BƯỚC 0: Đăng Ký VNPay Sandbox (5 phút)

### Làm thủ công (bạn tự làm):
- [ ] Truy cập: https://sandbox.vnpayment.vn/devreg/
- [ ] Điền form đăng ký (email, tên, SĐT)
- [ ] Xác nhận email
- [ ] Login vào dashboard
- [ ] Copy **TMN Code** (ví dụ: DEMOV210)
- [ ] Copy **Hash Secret** (ví dụ: RAOEXHYVSDDIIENYWSLDIIZTANXUXZFJ)

**LƯU Ý**: Lưu 2 thông tin này, sẽ dùng ở Bước 3!

---

## ✅ BƯỚC 1: Database Migration (Tôi làm)

### Files tôi sẽ tạo:
- [ ] `src/main/resources/db/migration/V2__add_payment_tables.sql`

### Bạn cần làm sau khi tôi tạo:
- [ ] Chạy: `./mvnw flyway:migrate`
- [ ] Verify: Check bảng `payments` đã được tạo

---

## ✅ BƯỚC 2: Add Dependencies (Tôi làm)

### Files tôi sẽ update:
- [ ] `pom.xml` - Thêm `commons-codec`

### Bạn cần làm sau khi tôi update:
- [ ] Chạy: `./mvnw clean install`

---

## ✅ BƯỚC 3: Configuration (Bạn làm)

### File cần update:
- [ ] `src/main/resources/application.properties`

### Thêm vào cuối file:
```properties
# VNPay Sandbox Configuration
vnpay.tmn-code=YOUR_TMN_CODE_HERE
vnpay.hash-secret=YOUR_HASH_SECRET_HERE
vnpay.url=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
vnpay.return-url=http://localhost:8080/payment-result.html
vnpay.ipn-url=http://localhost:8080/api/payment/vnpay-ipn
```

**QUAN TRỌNG**: Thay `YOUR_TMN_CODE_HERE` và `YOUR_HASH_SECRET_HERE` bằng credentials từ Bước 0!

---

## ✅ BƯỚC 4: Backend - Entity & Repository (Tôi làm)

### Files tôi sẽ tạo:
- [ ] `src/main/java/com/htai/exe201phapluatso/payment/entity/Payment.java`
- [ ] `src/main/java/com/htai/exe201phapluatso/payment/repo/PaymentRepo.java`

---

## ✅ BƯỚC 5: Backend - Configuration (Tôi làm)

### Files tôi sẽ tạo:
- [ ] `src/main/java/com/htai/exe201phapluatso/payment/config/VNPayConfig.java`

---

## ✅ BƯỚC 6: Backend - VNPay Service (Tôi làm)

### Files tôi sẽ tạo:
- [ ] `src/main/java/com/htai/exe201phapluatso/payment/service/VNPayService.java`
- [ ] `src/main/java/com/htai/exe201phapluatso/payment/util/VNPayUtil.java`

---

## ✅ BƯỚC 7: Backend - Payment Service (Tôi làm)

### Files tôi sẽ tạo:
- [ ] `src/main/java/com/htai/exe201phapluatso/payment/service/PaymentService.java`

---

## ✅ BƯỚC 8: Backend - DTOs (Tôi làm)

### Files tôi sẽ tạo:
- [ ] `src/main/java/com/htai/exe201phapluatso/payment/dto/CreatePaymentRequest.java`
- [ ] `src/main/java/com/htai/exe201phapluatso/payment/dto/CreatePaymentResponse.java`
- [ ] `src/main/java/com/htai/exe201phapluatso/payment/dto/PaymentCallbackRequest.java`

---

## ✅ BƯỚC 9: Backend - Controller (Tôi làm)

### Files tôi sẽ tạo:
- [ ] `src/main/java/com/htai/exe201phapluatso/payment/controller/PaymentController.java`

---

## ✅ BƯỚC 10: Frontend - Update Plans Page (HOÀN THÀNH)

### Files đã update:
- [x] `src/main/resources/static/html/plans.html` - Đã thêm payment logic

---

## ✅ BƯỚC 11: Frontend - Payment Result Page (HOÀN THÀNH)

### Files đã tạo:
- [x] `src/main/resources/static/html/payment-result.html` - Complete với success/failed states

---

## ✅ BƯỚC 12: Build & Run (Bạn làm)

### Commands:
- [ ] `./mvnw clean package -DskipTests`
- [ ] `./mvnw spring-boot:run`
- [ ] Verify: Server chạy OK trên port 8080

---

## ✅ BƯỚC 13: Test Payment Flow (Bạn làm)

### Test steps:
1. [ ] Mở browser: http://localhost:8080/html/plans.html
2. [ ] Login vào hệ thống
3. [ ] Click nút "Mua ngay" ở gói REGULAR hoặc STUDENT
4. [ ] Verify: Redirect sang VNPay sandbox
5. [ ] Chọn ngân hàng: **NCB**
6. [ ] Nhập thông tin thẻ test:
   ```
   Số thẻ: 9704198526191432198
   Tên: NGUYEN VAN A
   Ngày: 07/15
   OTP: 123456
   ```
7. [ ] Click "Thanh toán"
8. [ ] Verify: Redirect về payment-result.html
9. [ ] Verify: Hiển thị "Thanh toán thành công"
10. [ ] Verify: Credits được cộng vào tài khoản

---

## ✅ BƯỚC 14: Verify Database (Bạn làm)

### Check database:
- [ ] Mở SQL Server Management Studio
- [ ] Query: `SELECT * FROM payments ORDER BY created_at DESC`
- [ ] Verify: Payment record với status = 'SUCCESS'
- [ ] Query: `SELECT * FROM user_credits WHERE user_id = YOUR_USER_ID`
- [ ] Verify: Credits đã được cộng
- [ ] Query: `SELECT * FROM credit_transactions ORDER BY created_at DESC`
- [ ] Verify: Transaction log đã được tạo

---

## 🎉 HOÀN THÀNH!

Sau khi hoàn thành tất cả bước trên, bạn đã có:
- ✅ Payment system hoạt động với VNPay Sandbox
- ✅ Test được thanh toán không mất tiền thật
- ✅ Credits tự động cộng sau khi thanh toán
- ✅ UI/UX hoàn chỉnh

---

## 🚀 Nâng Cấp Lên Production (Sau này)

Khi muốn nhận tiền thật:
1. Đăng ký VNPay Production merchant
2. Nhận TMN Code & Hash Secret mới
3. Update `application.properties`:
   ```properties
   vnpay.tmn-code=PRODUCTION_TMN_CODE
   vnpay.hash-secret=PRODUCTION_HASH_SECRET
   vnpay.url=https://pay.vnpay.vn/vpcpay.html
   ```
4. Deploy lên server production
5. Done!

---

## 📊 Tổng Quan Files Sẽ Tạo

```
src/main/
├── java/com/htai/exe201phapluatso/
│   └── payment/
│       ├── entity/
│       │   └── Payment.java
│       ├── repo/
│       │   └── PaymentRepo.java
│       ├── service/
│       │   ├── VNPayService.java
│       │   └── PaymentService.java
│       ├── controller/
│       │   └── PaymentController.java
│       ├── config/
│       │   └── VNPayConfig.java
│       ├── dto/
│       │   ├── CreatePaymentRequest.java
│       │   ├── CreatePaymentResponse.java
│       │   └── PaymentCallbackRequest.java
│       └── util/
│           └── VNPayUtil.java
└── resources/
    ├── db/migration/
    │   └── V2__add_payment_tables.sql
    └── static/
        ├── html/
        │   └── payment-result.html
        ├── scripts/
        │   ├── plans.js
        │   └── payment-result.js
        └── css/
            └── payment-result.css
```

**Tổng cộng**: ~15 files

---

## ⏱️ Timeline Ước Tính

- **Bước 0** (Bạn): 5 phút
- **Bước 1-11** (Tôi): 30 phút (tạo code)
- **Bước 3** (Bạn): 2 phút (update config)
- **Bước 12** (Bạn): 5 phút (build & run)
- **Bước 13-14** (Bạn): 10 phút (test)

**Tổng**: ~50 phút

---

## 🎯 Sẵn Sàng Bắt Đầu?

**Bạn cần làm ngay**:
1. Đăng ký VNPay Sandbox (Bước 0)
2. Lưu TMN Code & Hash Secret
3. Báo tôi khi xong → Tôi sẽ tạo tất cả code!

**Hoặc**:
- Nếu bạn đã có credentials → Tôi tạo code ngay!
- Nếu chưa → Làm Bước 0 trước, mất 5 phút thôi!

Bạn muốn tôi **bắt đầu tạo code ngay** hay **đợi bạn đăng ký Sandbox trước**?
