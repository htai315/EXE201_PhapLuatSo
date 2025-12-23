# VNPay Account Setup Guide

## TL;DR
**CÓ**, bạn cần đăng ký tài khoản merchant với VNPay để nhận tiền thật. Nhưng có thể test miễn phí với Sandbox trước!

## 2 Môi Trường VNPay

### 1. SANDBOX (Test Environment) - MIỄN PHÍ ✅
**Dùng để**: Develop và test, không cần đăng ký gì cả!

**Cách dùng**:
- Truy cập: https://sandbox.vnpayment.vn/
- Đăng ký tài khoản test (miễn phí, tự động)
- Nhận ngay:
  - `TMN Code` (Mã merchant test)
  - `Hash Secret` (Key để mã hóa)
- Test với thẻ test của VNPay (không mất tiền thật)

**Thẻ test VNPay cung cấp**:
```
Ngân hàng: NCB
Số thẻ: 9704198526191432198
Tên chủ thẻ: NGUYEN VAN A
Ngày phát hành: 07/15
Mật khẩu OTP: 123456
```

**Ưu điểm**:
- ✅ Hoàn toàn miễn phí
- ✅ Test đầy đủ flow thanh toán
- ✅ Không cần giấy tờ, hợp đồng
- ✅ Có thể dùng vô thời hạn

**Nhược điểm**:
- ❌ Không nhận tiền thật
- ❌ Chỉ dùng để test

### 2. PRODUCTION (Real Environment) - CẦN ĐĂNG KÝ 📝
**Dùng để**: Nhận tiền thật từ khách hàng

**Quy trình đăng ký**:

#### Bước 1: Đăng ký Merchant
1. Truy cập: https://vnpay.vn/dang-ky-merchant/
2. Điền form đăng ký với thông tin:
   - Tên công ty/cá nhân
   - Mã số thuế (nếu có)
   - Địa chỉ kinh doanh
   - Số điện thoại, email
   - Website/App URL
   - Loại hình kinh doanh

#### Bước 2: Chuẩn bị giấy tờ
**Doanh nghiệp**:
- Giấy phép kinh doanh
- Mã số thuế
- Giấy tờ pháp nhân
- Thông tin tài khoản ngân hàng nhận tiền

**Cá nhân** (nếu VNPay chấp nhận):
- CMND/CCCD
- Giấy tờ chứng minh hoạt động kinh doanh
- Tài khoản ngân hàng cá nhân

#### Bước 3: Ký hợp đồng
- VNPay sẽ liên hệ để ký hợp đồng
- Thời gian xét duyệt: 3-7 ngày làm việc
- Phí dịch vụ: **1.5% - 3%** mỗi giao dịch (tùy thỏa thuận)

#### Bước 4: Nhận credentials
Sau khi được duyệt:
- `TMN Code` (Production)
- `Hash Secret` (Production)
- Tài liệu API
- Hỗ trợ kỹ thuật

## Chi Phí Sử Dụng VNPay

### Phí giao dịch
- **Thẻ nội địa**: 1.5% - 2.5%
- **Thẻ quốc tế**: 2.5% - 3.5%
- **QR Code**: 0.5% - 1.5%

### Ví dụ tính phí
```
Gói REGULAR: 159,000 VNĐ
Phí VNPay (2%): 3,180 VNĐ
Bạn nhận: 155,820 VNĐ

Gói STUDENT: 249,000 VNĐ
Phí VNPay (2%): 4,980 VNĐ
Bạn nhận: 244,020 VNĐ
```

### Phí khác
- Phí setup: **MIỄN PHÍ** (thường)
- Phí duy trì: **MIỄN PHÍ**
- Phí rút tiền: Tùy ngân hàng

## Lộ Trình Recommend

### Phase 1: Development (1-2 tuần)
```
✅ Dùng VNPay SANDBOX
✅ Implement toàn bộ code
✅ Test đầy đủ các flows
✅ Fix bugs
✅ UI/UX hoàn thiện
```

### Phase 2: Pre-Production (1 tuần)
```
📝 Đăng ký tài khoản VNPay Production
📝 Chuẩn bị giấy tờ
📝 Chờ duyệt (3-7 ngày)
```

### Phase 3: Production (1 ngày)
```
🔄 Thay credentials từ Sandbox → Production
🔄 Deploy lên server production
🔄 Test với tiền thật (số tiền nhỏ)
✅ Go live!
```

## Alternative: Các Cổng Thanh Toán Khác

Nếu VNPay khó đăng ký, có thể xem xét:

### 1. **MoMo** (Dễ đăng ký hơn)
- Phí: 1.5% - 2%
- Dễ dàng cho cá nhân
- API đơn giản
- Link: https://business.momo.vn/

### 2. **ZaloPay**
- Phí: 1.5% - 2.5%
- Tích hợp dễ
- Phổ biến với Gen Z
- Link: https://zalopay.vn/business

### 3. **PayOS** (Cực dễ)
- Phí: 2% - 3%
- Đăng ký online 100%
- Không cần giấy tờ phức tạp
- Dành cho startup/cá nhân
- Link: https://payos.vn/

### 4. **Stripe** (Quốc tế)
- Phí: 3.4% + 10,000đ
- Hỗ trợ thẻ quốc tế
- Cần giấy tờ doanh nghiệp
- Link: https://stripe.com/

## Recommendation Cho Bạn

### Nếu bạn là **Sinh viên/Cá nhân**:
1. **Bắt đầu với Sandbox** - Code và test miễn phí
2. **Xem xét PayOS hoặc MoMo** - Dễ đăng ký hơn VNPay
3. **Sau khi có doanh thu** - Chuyển sang VNPay (phí thấp hơn)

### Nếu bạn có **Công ty/MST**:
1. **Dùng Sandbox** để develop
2. **Đăng ký VNPay Production** song song
3. **Deploy production** khi được duyệt

## Code Implementation Strategy

### Thiết kế linh hoạt để dễ đổi payment gateway:

```java
// Interface chung
public interface PaymentGateway {
    String createPaymentUrl(PaymentRequest request);
    PaymentResult verifyPayment(Map<String, String> params);
}

// VNPay implementation
@Service
public class VNPayGateway implements PaymentGateway {
    // VNPay specific code
}

// MoMo implementation (future)
@Service
public class MoMoGateway implements PaymentGateway {
    // MoMo specific code
}

// Service sử dụng
@Service
public class PaymentService {
    @Autowired
    @Qualifier("vnpay") // Có thể đổi thành "momo"
    private PaymentGateway paymentGateway;
}
```

## Câu Hỏi Thường Gặp

### Q: Có thể test mà không cần đăng ký gì không?
**A**: CÓ! Dùng VNPay Sandbox hoàn toàn miễn phí, không cần đăng ký phức tạp.

### Q: Phải có công ty mới dùng được VNPay?
**A**: Không bắt buộc, nhưng có công ty sẽ dễ dàng hơn. Cá nhân có thể dùng MoMo hoặc PayOS.

### Q: Mất bao lâu để được duyệt?
**A**: 3-7 ngày làm việc nếu giấy tờ đầy đủ.

### Q: Có thể dùng nhiều payment gateway cùng lúc?
**A**: CÓ! Nên thiết kế code để hỗ trợ nhiều gateway, user chọn cái nào họ thích.

### Q: Sandbox có giống Production 100%?
**A**: Gần như 100%, chỉ khác:
- URL endpoint
- Credentials (TMN Code, Hash Secret)
- Không nhận tiền thật

## Next Steps

**Ngay bây giờ**:
1. ✅ Dùng Sandbox để develop
2. ✅ Implement code hoàn chỉnh
3. ✅ Test kỹ càng

**Khi sẵn sàng production**:
1. 📝 Đăng ký VNPay/MoMo/PayOS
2. 🔄 Thay credentials
3. 🚀 Deploy

**Bạn muốn tôi**:
- Hướng dẫn đăng ký Sandbox ngay?
- Implement code với Sandbox trước?
- So sánh chi tiết các payment gateway?
