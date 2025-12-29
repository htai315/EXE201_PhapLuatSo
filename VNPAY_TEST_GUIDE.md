# VNPay Payment Integration - Complete Test Guide 🚀

## ✅ All Issues Fixed

### 1. Application Properties Configuration
- ✅ VNPay config added to `target/classes/application.properties`
- ✅ TMN Code: `GSKRGDM2`
- ✅ Hash Secret: `SCIB5A0QTYDULYE523L2ZA8ZOHM4CDXW`

### 2. Security Configuration
- ✅ `/api/payment/vnpay-ipn` is now PUBLIC (VNPay server callback)
- ✅ `/api/payment/create` requires JWT authentication (user payment request)
- ✅ `/html/plans.html` is accessible to all users

### 3. Backend Implementation
- ✅ Payment entity and repository
- ✅ VNPayConfig, VNPayUtil, VNPayService
- ✅ PaymentService with credit integration
- ✅ PaymentController with create and IPN endpoints

### 4. Frontend Implementation
- ✅ Plans page with payment buttons
- ✅ Payment result page with success/failure states
- ✅ JavaScript payment flow with JWT token

---

## 🧪 Testing Steps

### Step 1: Start Application
1. Open IntelliJ IDEA
2. Run `Exe201PhapLuatSoApplication.main()`
3. Wait for application to start on port 8080
4. Check console for "Started Exe201PhapLuatSoApplication" message

### Step 2: Login to Your Account
1. Go to: http://localhost:8080/html/login.html
2. Login with your credentials
3. Verify you're logged in (see avatar in navbar)

### Step 3: Go to Plans Page
1. Navigate to: http://localhost:8080/html/plans.html
2. You should see 3 plans: FREE, REGULAR, STUDENT
3. FREE plan shows "Đã kích hoạt" (already activated)
4. REGULAR and STUDENT plans show "Chọn gói này" button

### Step 4: Select a Plan
1. Click "Chọn gói này" on REGULAR plan (159,000 VND)
2. Button should change to "Đang xử lý..." (processing)
3. You will be redirected to VNPay Sandbox payment page

### Step 5: Complete Payment on VNPay Sandbox
**VNPay Test Card Information:**
- **Card Number**: `9704198526191432198`
- **Card Holder Name**: `NGUYEN VAN A`
- **Expiry Date**: `07/15`
- **OTP Code**: `123456`

**Payment Steps:**
1. Enter card number: `9704198526191432198`
2. Enter card holder: `NGUYEN VAN A`
3. Enter expiry date: `07/15`
4. Click "Thanh toán" (Pay)
5. Enter OTP: `123456`
6. Click "Xác nhận" (Confirm)

### Step 6: Payment Result
1. After successful payment, you'll be redirected to: http://localhost:8080/payment-result.html
2. You should see:
   - ✅ Success animation
   - "Thanh toán thành công!" message
   - Transaction details
   - Countdown timer (redirecting to profile in 10 seconds)
3. Click "Xem hồ sơ" or wait for auto-redirect

### Step 7: Verify Credits Added
1. Go to: http://localhost:8080/html/profile.html
2. Check "Thông tin Credits" section
3. You should see:
   - **Chat Credits**: 110 (10 FREE + 100 REGULAR)
   - **Quiz Credits**: 0
   - **Expiry Date**: 12 months from now
4. Check credits counter in navbar (should show 110)

### Step 8: Verify Database Records
Open SQL Server Management Studio and run:

```sql
-- Check payment record
SELECT * FROM payment ORDER BY created_at DESC;

-- Check credit transaction
SELECT * FROM credit_transaction ORDER BY created_at DESC;

-- Check user credits
SELECT * FROM user_credit WHERE user_id = YOUR_USER_ID;
```

You should see:
- New payment record with status 'SUCCESS'
- New credit transaction with type 'PURCHASE'
- Updated user_credit with new balance and expiry date

---

## 🎯 Expected Results

### Payment Record
```
payment_id: PAY_xxxxxx
user_id: your_user_id
plan_id: 2 (REGULAR)
amount: 159000.00
status: SUCCESS
vnpay_txn_ref: PAY_xxxxxx
vnpay_transaction_no: xxxxxxxx
vnpay_bank_code: NCB
vnpay_card_type: ATM
```

### Credit Transaction
```
transaction_id: auto-generated
user_id: your_user_id
type: PURCHASE
amount: 100
description: Mua gói REGULAR - 100 chat credits
```

### User Credit
```
chat_credits: 110 (10 FREE + 100 REGULAR)
quiz_credits: 0
expires_at: 2026-12-29 (12 months from now)
```

---

## 🔍 Troubleshooting

### Issue: Application won't start
**Error**: `Could not resolve placeholder 'vnpay.tmn-code'`

**Solution**:
```bash
# Copy application.properties to target/classes
copy src\main\resources\application.properties target\classes\application.properties
```

### Issue: Redirected to login page when clicking "Nâng cấp"
**Cause**: Not logged in or JWT token expired

**Solution**:
1. Login again
2. Check browser console for errors
3. Verify JWT token in localStorage

### Issue: Payment fails on VNPay
**Cause**: Wrong test card information

**Solution**: Use exact test card details:
- Card: `9704198526191432198`
- Name: `NGUYEN VAN A`
- Expiry: `07/15`
- OTP: `123456`

### Issue: Credits not added after payment
**Cause**: VNPay IPN callback failed

**Solution**:
1. Check application logs for IPN callback
2. Verify payment status in database
3. Check if signature verification passed

---

## 📊 Payment Flow Diagram

```
User (Browser)                    Backend                    VNPay
     |                               |                          |
     |--[1] Click "Nâng cấp"-------->|                          |
     |                               |                          |
     |<--[2] Show "Đang xử lý..."---|                          |
     |                               |                          |
     |--[3] POST /api/payment/create |                          |
     |    (with JWT token)---------->|                          |
     |                               |                          |
     |                               |--[4] Create payment----->|
     |                               |    record in DB          |
     |                               |                          |
     |                               |--[5] Generate payment--->|
     |                               |    URL with signature    |
     |                               |                          |
     |<--[6] Return payment URL-----|                          |
     |                               |                          |
     |--[7] Redirect to VNPay------->|                          |
     |                               |                          |
     |                               |                          |
     |--[8] Enter card info--------->|------------------------->|
     |                               |                          |
     |--[9] Enter OTP--------------->|------------------------->|
     |                               |                          |
     |                               |<--[10] IPN callback------|
     |                               |    (server-to-server)    |
     |                               |                          |
     |                               |--[11] Verify signature-->|
     |                               |                          |
     |                               |--[12] Update payment---->|
     |                               |    status to SUCCESS     |
     |                               |                          |
     |                               |--[13] Add credits------->|
     |                               |    to user account       |
     |                               |                          |
     |<--[14] Redirect to result----|--------------------------|
     |    page with success         |                          |
     |                               |                          |
```

---

## 🎉 Success Criteria

✅ Application starts without errors
✅ Can access plans page while logged in
✅ Payment button redirects to VNPay Sandbox
✅ Can complete payment with test card
✅ Redirected to success page after payment
✅ Credits added to user account automatically
✅ Payment record saved in database
✅ Credit transaction recorded
✅ Credits counter updated in navbar

---

## 📝 Notes

- **VNPay Sandbox is FREE** - no real money involved
- Test card always succeeds with correct OTP
- Payment URL expires after 15 minutes
- Credits expire after 12 months from purchase date
- Multiple purchases stack credits and extend expiry date
- IPN callback is server-to-server (no JWT needed)
- Return URL is user redirect (shows result page)

---

## 🚀 Ready to Test!

All code is complete and configuration is fixed. You can now:
1. Start the application in IntelliJ
2. Test the complete payment flow
3. Verify credits are added correctly

Good luck with testing! 🎯
