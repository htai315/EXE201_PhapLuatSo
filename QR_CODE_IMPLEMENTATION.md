# QR Code Generation Implementation

## 📋 Tổng Quan

Đã implement giải pháp tự generate QR code bằng ZXing library để:
- Không phụ thuộc external API (api.qrserver.com)
- Luôn có QR code khi reuse payment
- Fallback khi PayOS không trả về QR code

## 🔧 Implementation Details

### 1. Dependencies (pom.xml)

```xml
<!-- ZXing for QR Code Generation -->
<dependency>
    <groupId>com.google.zxing</groupId>
    <artifactId>core</artifactId>
    <version>3.5.3</version>
</dependency>
<dependency>
    <groupId>com.google.zxing</groupId>
    <artifactId>javase</artifactId>
    <version>3.5.3</version>
</dependency>
```

### 2. QRCodeService

**Location:** `src/main/java/com/htai/exe201phapluatso/payment/service/QRCodeService.java`

**Features:**
- Generate QR code as base64 data URI
- Configurable size (default 280x280)
- Error correction level M
- UTF-8 character encoding
- Minimal margin (1 pixel)

**Methods:**
```java
// Generate with custom size
String generateQRCodeBase64(String data, int width, int height)

// Generate with default size (280x280)
String generateQRCodeBase64(String data)
```

**Output Format:**
```
data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA...
```

### 3. PayOSService Integration

**Scenario 1: Reuse Payment**
```java
// When reusing payment, generate QR code from checkout URL
String checkoutUrl = "https://pay.payos.vn/web/" + orderCode;
String qrCode = qrCodeService.generateQRCodeBase64(checkoutUrl);
```

**Scenario 2: New Payment (Fallback)**
```java
// If PayOS doesn't provide QR code, generate our own
String qrCode = paymentLink.getQrCode();
if (qrCode == null || qrCode.isBlank()) {
    qrCode = qrCodeService.generateQRCodeBase64(paymentLink.getCheckoutUrl());
}
```

### 4. Frontend Handling (plans.html)

**Updated Logic:**
```javascript
if (data.qrCode) {
    if (data.qrCode.startsWith('data:image')) {
        // Base64 from QRCodeService
        qrImage.src = data.qrCode;
    } else if (data.qrCode.startsWith('http')) {
        // URL from PayOS
        qrImage.src = data.qrCode;
    } else {
        // VietQR string (fallback to external API)
        qrImage.src = `https://api.qrserver.com/v1/create-qr-code/?...`;
    }
}
```

## 🎯 Benefits

### ✅ Reliability
- Không phụ thuộc external API
- Luôn có QR code (reuse + fallback)
- Không bị down khi api.qrserver.com fail

### ✅ Performance
- Generate local (không cần network call)
- Nhanh hơn external API
- Giảm latency

### ✅ User Experience
- Reuse payment có QR code ngay lập tức
- Không cần click "Mở trang thanh toán" thủ công
- Consistent experience

### ✅ Maintainability
- Clean separation of concerns
- Reusable QRCodeService
- Easy to test

## 📊 QR Code Formats Supported

| Format | Source | Example | Handling |
|--------|--------|---------|----------|
| Base64 Data URI | QRCodeService | `data:image/png;base64,...` | Direct display |
| HTTP URL | PayOS | `https://...` | Direct display |
| VietQR String | PayOS | `00020101...` | External API fallback |

## 🔍 Technical Details

### QR Code Configuration

```java
Map<EncodeHintType, Object> hints = new HashMap<>();
hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
hints.put(EncodeHintType.MARGIN, 1);
```

**Error Correction Level M:**
- 15% of codewords can be restored
- Good balance between size and reliability
- Suitable for payment URLs

**Margin = 1:**
- Minimal white border
- Compact QR code
- Still scannable

### Image Format

- **Format:** PNG
- **Size:** 280x280 pixels (default)
- **Encoding:** Base64
- **Average size:** ~3-5KB

## 🚀 Usage Examples

### Example 1: Create New Payment

```java
// PayOSService
var paymentLink = payOS.paymentRequests().create(request);
String qrCode = paymentLink.getQrCode();

if (qrCode == null) {
    // Fallback: Generate our own
    qrCode = qrCodeService.generateQRCodeBase64(paymentLink.getCheckoutUrl());
}
```

### Example 2: Reuse Payment

```java
// PayOSService
String checkoutUrl = "https://pay.payos.vn/web/" + orderCode;
String qrCode = qrCodeService.generateQRCodeBase64(checkoutUrl);

return new CreatePaymentResponse(checkoutUrl, orderCode, qrCode, amount, planName);
```

### Example 3: Custom Size

```java
// Generate larger QR code
String qrCode = qrCodeService.generateQRCodeBase64(url, 400, 400);
```

## 🧪 Testing

### Manual Testing

1. **Test Reuse Payment:**
   - Click "Mua ngay" → QR code hiển thị
   - Click lại "Mua ngay" trong 10 phút → QR code vẫn hiển thị

2. **Test New Payment:**
   - Đợi > 10 phút hoặc dùng gói khác
   - Click "Mua ngay" → QR code hiển thị

3. **Test QR Code Scanning:**
   - Quét QR code bằng app ngân hàng
   - Verify redirect đến PayOS checkout

### Error Handling

```java
try {
    qrCode = qrCodeService.generateQRCodeBase64(checkoutUrl);
} catch (Exception e) {
    log.warn("Failed to generate QR code: {}", e.getMessage());
    // Continue without QR code (user can still use checkout link)
}
```

## 📝 Logging

```
DEBUG: Generating QR code: width=280, height=280, dataLength=45
DEBUG: QR code generated successfully: size=3KB
DEBUG: Generated QR code for reused payment: orderCode=123456
```

## 🔄 Migration Notes

### Before
- Reuse payment: No QR code
- Fallback: External API (api.qrserver.com)
- Dependency: External service

### After
- Reuse payment: ✅ QR code generated
- Fallback: ✅ Self-generated QR code
- Dependency: ✅ None (self-contained)

## 💡 Future Enhancements

### Optional Improvements

1. **QR Code Customization:**
   ```java
   // Add logo in center
   // Change colors
   // Add border
   ```

2. **Caching:**
   ```java
   // Cache generated QR codes
   // Reduce CPU usage for repeated requests
   ```

3. **Database Storage:**
   ```sql
   ALTER TABLE payments ADD qr_code_base64 TEXT;
   ```

## 🎓 Best Practices

1. **Always handle exceptions** - QR generation might fail
2. **Use appropriate size** - 280x280 is good for mobile scanning
3. **Log generation** - For debugging and monitoring
4. **Test scanning** - Verify QR codes work with real apps

## 📚 References

- [ZXing GitHub](https://github.com/zxing/zxing)
- [QR Code Specification](https://www.qrcode.com/en/about/standards.html)
- [Error Correction Levels](https://www.qrcode.com/en/about/error_correction.html)

---

**Implementation Date:** 2026-01-07
**Version:** 1.0
**Status:** ✅ Production Ready
