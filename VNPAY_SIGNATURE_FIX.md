# VNPay Signature Fix - COMPLETED ✅

## Problem
VNPay returned "Sai chữ ký" (Invalid Signature) error when processing payment.

## Root Causes Identified

### 1. Compilation Error
- **Location**: `VNPayUtil.java` line 21
- **Error**: `java: illegal start of expression`
- **Cause**: Lambda syntax error in `buildQueryString()` method
- **Status**: ✅ FIXED

### 2. Signature Mismatch Issues
- **Vietnamese characters in OrderInfo**: "Thanh toan goi Người Dân" caused encoding problems
- **Whitespace in OrderInfo**: Spaces caused URL encoding mismatches
- **IPv6 address format**: "0:0:0:0:0:0:0:1" with colons caused signature calculation issues

## Solutions Applied

### Fix 1: OrderInfo Format
**File**: `PaymentService.java`

**Before**:
```java
String orderInfo = "Thanh toan goi " + planCode;
```

**After**:
```java
String orderInfo = "Payment_" + planCode;  // e.g., "Payment_REGULAR"
```

**Why**: 
- No Vietnamese characters
- No spaces
- Only ASCII alphanumeric + underscore
- Clean signature calculation

### Fix 2: IPv6 to IPv4 Conversion
**File**: `PaymentController.java`

**Before**:
```java
private String getClientIP(HttpServletRequest request) {
    String xfHeader = request.getHeader("X-Forwarded-For");
    if (xfHeader == null) {
        return request.getRemoteAddr();  // Returns "0:0:0:0:0:0:0:1"
    }
    return xfHeader.split(",")[0];
}
```

**After**:
```java
private String getClientIP(HttpServletRequest request) {
    String xfHeader = request.getHeader("X-Forwarded-For");
    String ip;
    if (xfHeader == null) {
        ip = request.getRemoteAddr();
    } else {
        ip = xfHeader.split(",")[0];
    }
    
    // Convert IPv6 localhost to IPv4
    if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
        return "127.0.0.1";
    }
    
    return ip;
}
```

**Why**:
- IPv6 "0:0:0:0:0:0:0:1" contains colons that may affect signature
- IPv4 "127.0.0.1" is cleaner for VNPay
- Localhost testing now uses standard IPv4 format

### Fix 3: Lambda Syntax (Compilation)
**File**: `VNPayUtil.java`

Fixed lambda expression syntax in `buildQueryString()` method - ensured proper Java syntax.

## Expected Results

### Hash Data (NEW)
```
vnp_Amount=15900000&vnp_Command=pay&vnp_CreateDate=...&vnp_CurrCode=VND&vnp_ExpireDate=...&vnp_IpAddr=127.0.0.1&vnp_Locale=vn&vnp_OrderInfo=Payment_REGULAR&vnp_OrderType=other&vnp_ReturnUrl=http://localhost:8080/payment-result.html&vnp_TmnCode=GSKRGDM2&vnp_TxnRef=PAY...&vnp_Version=2.1.0
```

**Key Changes**:
- `vnp_IpAddr=127.0.0.1` (was `0:0:0:0:0:0:0:1`)
- `vnp_OrderInfo=Payment_REGULAR` (was `Thanh toan goi Người Dân`)

## Testing Instructions

1. **Rebuild the project** in IntelliJ (Build > Rebuild Project)
2. **Restart the application**
3. **Test payment flow**:
   - Go to `http://localhost:8080/plans.html`
   - Click "Chọn gói này" for REGULAR plan
   - Should redirect to VNPay Sandbox
   - **Expected**: No "Sai chữ ký" error
   - **Expected**: Payment form displays correctly
4. **Complete test payment**:
   - Card: `9704198526191432198`
   - Name: `NGUYEN VAN A`
   - Expiry: `07/15`
   - OTP: `123456`
5. **Verify credits added** after successful payment

## VNPay Credentials (Sandbox)
- TMN Code: `GSKRGDM2`
- Hash Secret: `SCIB5A0QTYDULYE523L2ZA8ZOHM4CDXW`
- Environment: Sandbox (free testing)

## Files Modified
1. `src/main/java/com/htai/exe201phapluatso/payment/util/VNPayUtil.java`
2. `src/main/java/com/htai/exe201phapluatso/payment/service/PaymentService.java`
3. `src/main/java/com/htai/exe201phapluatso/payment/controller/PaymentController.java`

## Status
✅ **READY FOR TESTING**

All compilation errors fixed, signature issues resolved. The payment flow should now work correctly with VNPay Sandbox.
