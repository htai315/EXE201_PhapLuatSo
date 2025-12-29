# VNPay Signature Fix - Test Guide

## Changes Made

### 1. Fixed Signature Generation Method
**File**: `VNPayService.java`
- Changed from `buildHashData()` to `buildHashDataForPayment()` when creating payment URL
- This ensures NO URL decoding happens during signature generation for payment creation

### 2. Improved Query String Building
**File**: `VNPayUtil.java`
- Changed `buildQueryString()` to only URL encode special characters (URLs with `://`, `&`, `=`)
- Regular values like "Payment_REGULAR" are NOT encoded
- This matches VNPay's expected format

### 3. Added Amount Validation (Security)
**Files**: `PaymentService.java`, `PaymentController.java`
- Added `vnpAmount` parameter to IPN callback
- Validates received amount matches expected amount
- Prevents amount tampering attacks

### 4. Separate Hash Methods
**File**: `VNPayUtil.java`
- `buildHashDataForPayment()`: For creating payment URL (NO decoding)
- `buildHashData()`: For verifying IPN callback (WITH decoding)

## How to Test

### Step 1: Rebuild Application
1. In IntelliJ, click **Build** → **Rebuild Project**
2. Wait for build to complete
3. Verify `target/classes/application.properties` has NEW VNPay credentials:
   ```
   vnpay.tmn-code=NA128BPU
   vnpay.hash-secret=WNLMPOIMP9GO2ORARN9CMYVL5F6EA4GU
   ```

### Step 2: Start Application
1. Run the application in IntelliJ
2. Check console logs for:
   ```
   VNPay Configuration loaded:
   TMN Code: NA128BPU
   Hash Secret: WNLMPOIMP9GO2ORARN9CMYVL5F6EA4GU
   ```

### Step 3: Test Payment Flow
1. Open browser: `http://localhost:8080/plans.html`
2. Login with your account
3. Click "Chọn gói này" on REGULAR plan (159,000 VND)
4. **Check console logs** for:
   ```
   Hash data: vnp_Amount=15900000&vnp_Command=pay&vnp_CreateDate=...
   Generated signature: [64-char hex string]
   Query string: vnp_Amount=15900000&vnp_Command=pay&...
   Payment URL created: https://sandbox.vnpayment.vn/...
   ```

### Step 4: VNPay Sandbox
1. You should be redirected to VNPay Sandbox
2. **CRITICAL**: Check if you see payment form (NOT "Sai chữ ký" error)
3. If you see payment form → **SIGNATURE FIX SUCCESSFUL!** 🎉

### Step 5: Complete Test Payment
Use VNPay test card:
- **Card Number**: `9704198526191432198`
- **Card Holder**: `NGUYEN VAN A`
- **Expiry Date**: `07/15`
- **OTP**: `123456`

### Step 6: Verify Credits Added
1. After payment success, check console logs:
   ```
   Payment SUCCESS: txnRef=PAY..., transactionNo=...
   Credits added: user=X, chat=100, quiz=0
   ```
2. Refresh `profile.html` to see updated credits

## Expected Behavior

### ✅ SUCCESS Indicators
- No "Sai chữ ký" error from VNPay
- VNPay payment form displays correctly
- Payment completes successfully
- Credits added automatically
- IPN callback processed without errors

### ❌ FAILURE Indicators
- Still see "Sai chữ ký" error
- Redirect back to plans page immediately
- No payment form shown

## Troubleshooting

### If Still Getting "Sai chữ ký"

#### Option 1: Verify Hash Secret
1. Check email from VNPay Sandbox registration
2. Copy Hash Secret EXACTLY (no spaces, correct case)
3. Update `application.properties`:
   ```
   vnpay.hash-secret=WNLMPOIMP9GO2ORARN9CMYVL5F6EA4GU
   ```
4. Rebuild and restart

#### Option 2: Compare with VNPay Demo
1. Download VNPay official Java demo: https://sandbox.vnpayment.vn/apis/docs/huong-dan-tich-hop/
2. Compare signature generation logic
3. Test with their demo first to verify credentials work

#### Option 3: Contact VNPay Support
If credentials are correct but still failing:
1. Go to VNPay Sandbox dashboard
2. Contact support with:
   - TMN Code: `NA128BPU`
   - Error: "Sai chữ ký" (Invalid Signature)
   - Request verification of Hash Secret

## What Changed vs Previous Attempts

| Previous Attempts | Current Fix |
|------------------|-------------|
| Used same `buildHashData()` for both payment and IPN | Separate methods: `buildHashDataForPayment()` and `buildHashData()` |
| URL encoded ALL values | Only encode special characters (URLs) |
| No amount validation | Added amount validation in IPN |
| URL decoding in payment signature | NO decoding in payment signature |

## Next Steps After Success

1. ✅ Test all 3 plans (FREE, REGULAR, STUDENT)
2. ✅ Test payment failure scenario (cancel payment)
3. ✅ Verify credits expiration (12 months for paid plans)
4. ✅ Test double callback prevention
5. ✅ Add Return URL handler (optional, for better UX)
6. ✅ Production deployment planning

## Code Review Improvements Applied

Based on external AI code review:
- ✅ Separate hash methods for payment vs IPN
- ✅ Amount validation in IPN callback
- ✅ Proper URL encoding strategy
- ✅ Double callback prevention (already had)
- ⚠️ Return URL handler (optional, not critical)

**Overall Score**: 9.5/10 - Production ready after signature fix verified
