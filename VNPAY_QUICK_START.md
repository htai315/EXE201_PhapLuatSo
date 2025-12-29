# VNPay Quick Start - Test Now!

## 🚀 Quick Test (5 Minutes)

### 1. Rebuild Project
In IntelliJ:
- Click **Build** → **Rebuild Project**
- Wait for "Build completed successfully"

### 2. Start Application
- Click **Run** button (green play icon)
- Wait for "Started Exe201PhapLuatSoApplication"

### 3. Test Payment
1. Open browser: `http://localhost:8080/plans.html`
2. Login with your account
3. Click **"Chọn gói này"** on REGULAR plan (159,000 VND)

### 4. Check Result
**✅ SUCCESS**: You see VNPay payment form with card input fields

**❌ FAILURE**: You see "Sai chữ ký" error or redirect back immediately

## 💳 Complete Test Payment

If you see the payment form:

1. **Card Number**: `9704198526191432198`
2. **Card Holder**: `NGUYEN VAN A`
3. **Expiry Date**: `07/15`
4. Click **Continue**
5. **OTP**: `123456`
6. Click **Confirm**

## ✅ Verify Success

1. Should redirect to success page
2. Go to `http://localhost:8080/profile.html`
3. Check credits balance (should show +100 chat credits)

## 🔍 Check Console Logs

Look for these messages:

**Payment Creation**:
```
Creating payment: user=X, plan=REGULAR, ip=127.0.0.1
Hash data: vnp_Amount=15900000&vnp_Command=pay&...
Generated signature: [long hex string]
Payment URL created: https://sandbox.vnpayment.vn/...
```

**IPN Callback** (after payment):
```
VNPay IPN received: {vnp_Amount=15900000, vnp_ResponseCode=00, ...}
Payment SUCCESS: txnRef=PAY..., transactionNo=...
Credits added: user=X, chat=100, quiz=0
```

## 🎯 What Changed?

### The Fix
Changed signature generation to use separate methods:
- **Payment creation**: NO URL decoding
- **IPN verification**: WITH URL decoding

This matches VNPay's expected format.

### Files Modified
1. `VNPayUtil.java` - Added `buildHashDataForPayment()` method
2. `VNPayService.java` - Use correct method for payment creation
3. `PaymentService.java` - Added amount validation
4. `PaymentController.java` - Pass amount to validation

## ❓ If Still Failing

### Option 1: Verify Credentials
Check `target/classes/application.properties`:
```properties
vnpay.tmn-code=NA128BPU
vnpay.hash-secret=WNLMPOIMP9GO2ORARN9CMYVL5F6EA4GU
```

If different, manually copy from `src/main/resources/application.properties`

### Option 2: Clean Rebuild
1. **Build** → **Clean Project**
2. **Build** → **Rebuild Project**
3. Restart application

### Option 3: Check Hash Secret
1. Find VNPay registration email
2. Copy Hash Secret EXACTLY (no spaces)
3. Update `application.properties`
4. Rebuild and restart

## 📊 Test All Plans

After first success, test other plans:

| Plan | Price | Expected Credits |
|------|-------|-----------------|
| REGULAR | 159,000 VND | +100 chat |
| STUDENT | 249,000 VND | +100 chat, +20 quiz |

## 🎉 Success Checklist

- [ ] No "Sai chữ ký" error
- [ ] VNPay payment form displays
- [ ] Test payment completes
- [ ] Credits added to account
- [ ] Profile page shows updated balance
- [ ] Console logs show IPN callback

## 📞 Need Help?

If still getting signature error after this fix:
1. Share console logs (payment creation + IPN)
2. Verify Hash Secret from VNPay email
3. Try VNPay official Java demo with same credentials

---

**Ready?** Rebuild → Run → Test! 🚀
