# QR Code Testing Checklist

## 🧪 Pre-Testing Setup

- [ ] Build project: `mvnw clean install`
- [ ] Verify no compilation errors
- [ ] Check ZXing dependencies downloaded
- [ ] Start application: `mvnw spring-boot:run`
- [ ] Verify application starts successfully

## 📋 Test Scenarios

### Scenario 1: New Payment (First Time)

**Steps:**
1. [ ] Login vào hệ thống
2. [ ] Navigate to `/html/plans.html`
3. [ ] Click "Chọn Gói Này" cho gói REGULAR
4. [ ] Verify modal hiển thị
5. [ ] Verify QR code hiển thị (không blank)
6. [ ] Verify QR code có thể scan được
7. [ ] Check browser console - không có errors

**Expected Result:**
- ✅ QR code hiển thị ngay lập tức
- ✅ QR code format: base64 hoặc URL
- ✅ Có thể quét bằng app ngân hàng

**Logs to Check:**
```
DEBUG: Generating QR code: width=280, height=280
DEBUG: QR code generated successfully: size=3KB
INFO: PayOS Response: checkoutUrl=..., qrCode=...
```

---

### Scenario 2: Reuse Payment (Click Again)

**Steps:**
1. [ ] Sau khi test Scenario 1
2. [ ] Close modal (không thanh toán)
3. [ ] Click lại "Chọn Gói Này" cho cùng gói REGULAR
4. [ ] Verify modal hiển thị
5. [ ] **Verify QR code hiển thị** (đây là test chính!)
6. [ ] Verify orderCode giống lần trước
7. [ ] Check browser console

**Expected Result:**
- ✅ QR code hiển thị (KHÔNG null như trước)
- ✅ OrderCode giống lần 1
- ✅ Message: "REUSING payment link"

**Logs to Check:**
```
INFO: Found recent pending payment: orderCode=123456
INFO: REUSING payment link: orderCode=123456
DEBUG: Generated QR code for reused payment: orderCode=123456
```

---

### Scenario 3: Different Plan

**Steps:**
1. [ ] Click "Chọn Gói Này" cho gói STUDENT (khác gói)
2. [ ] Verify modal hiển thị
3. [ ] Verify QR code hiển thị
4. [ ] Verify orderCode khác với lần trước

**Expected Result:**
- ✅ Tạo payment mới (không reuse)
- ✅ OrderCode mới
- ✅ QR code hiển thị

---

### Scenario 4: Expired Payment (After 10 Minutes)

**Steps:**
1. [ ] Đợi > 10 phút (hoặc change config `payment.spam-block-minutes=1`)
2. [ ] Click "Chọn Gói Này" cho cùng gói
3. [ ] Verify tạo payment mới
4. [ ] Verify QR code hiển thị

**Expected Result:**
- ✅ Tạo payment mới
- ✅ OrderCode mới
- ✅ QR code hiển thị

---

### Scenario 5: QR Code Scanning

**Steps:**
1. [ ] Mở app ngân hàng (VietQR compatible)
2. [ ] Quét QR code từ modal
3. [ ] Verify redirect đến PayOS checkout page
4. [ ] Verify thông tin đúng (amount, orderCode)

**Expected Result:**
- ✅ QR code scannable
- ✅ Redirect đến PayOS
- ✅ Thông tin chính xác

---

### Scenario 6: PayOS Không Trả QR (Fallback)

**Test này cần mock PayOS response**

**Steps:**
1. [ ] Temporarily modify PayOSService để force `qrCode = null`
2. [ ] Click "Chọn Gói Này"
3. [ ] Verify QR code vẫn hiển thị (fallback)
4. [ ] Restore code

**Expected Result:**
- ✅ Fallback QR generation works
- ✅ QR code hiển thị

**Logs to Check:**
```
INFO: PayOS did not provide QR code, generating our own
DEBUG: Generated fallback QR code for orderCode=123456
```

---

## 🔍 Visual Inspection

### QR Code Quality
- [ ] QR code không bị blur
- [ ] Size phù hợp (280x280)
- [ ] Có white border minimal
- [ ] Contrast tốt (đen/trắng rõ ràng)

### Modal UI
- [ ] QR code centered
- [ ] Loading spinner hoạt động
- [ ] Checkout link clickable
- [ ] OrderCode hiển thị đúng
- [ ] Amount format đúng (VNĐ)

---

## 🐛 Error Cases

### Test Error Handling

**Case 1: Invalid URL**
```java
// Temporarily test with invalid URL
qrCodeService.generateQRCodeBase64("invalid-url");
```
- [ ] Verify exception caught
- [ ] Verify log warning
- [ ] Verify app không crash

**Case 2: Very Long URL**
```java
// Test with very long URL (>1000 chars)
String longUrl = "https://..." + "x".repeat(1000);
qrCodeService.generateQRCodeBase64(longUrl);
```
- [ ] Verify QR code generated
- [ ] Verify size reasonable

---

## 📊 Performance Testing

### QR Generation Speed
- [ ] Check logs for generation time
- [ ] Should be < 100ms
- [ ] No noticeable delay in UI

### Memory Usage
- [ ] Generate 10 QR codes
- [ ] Check memory usage (should be stable)
- [ ] No memory leaks

---

## 🔄 Regression Testing

### Existing Functionality
- [ ] Payment creation still works
- [ ] Webhook processing still works
- [ ] Payment history still works
- [ ] Status polling still works
- [ ] Cancel payment still works

### Backward Compatibility
- [ ] PayOS URL QR code still works
- [ ] VietQR string still works
- [ ] External API fallback still works

---

## 📝 Browser Compatibility

Test on multiple browsers:
- [ ] Chrome
- [ ] Firefox
- [ ] Edge
- [ ] Safari (if available)
- [ ] Mobile browsers

---

## ✅ Final Checklist

- [ ] All test scenarios passed
- [ ] No console errors
- [ ] No server errors
- [ ] QR codes scannable
- [ ] Performance acceptable
- [ ] UI looks good
- [ ] Logs are clean

---

## 🚨 Known Issues

Document any issues found:

1. **Issue:** 
   - **Description:** 
   - **Severity:** 
   - **Workaround:** 

---

## 📸 Screenshots

Take screenshots of:
- [ ] QR code modal (new payment)
- [ ] QR code modal (reused payment)
- [ ] Successful scan result
- [ ] Browser console (no errors)

---

## 🎯 Success Criteria

✅ **Must Have:**
- Reuse payment có QR code
- QR code scannable
- No errors in console/logs
- Performance acceptable

✅ **Nice to Have:**
- QR code quality excellent
- Fast generation (<50ms)
- Clean logs

---

**Tester:** _____________
**Date:** _____________
**Result:** ⬜ PASS / ⬜ FAIL
**Notes:** 

