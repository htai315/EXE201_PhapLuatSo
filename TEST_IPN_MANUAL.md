# Test IPN Callback Manually

## Vấn Đề
- Payment thành công ✅
- Redirect về payment-result.html ✅
- Nhưng IPN callback chưa chạy (chưa thấy log trong console)

## Giải Pháp: Test IPN Manual

### Bước 1: Mở Browser Mới (hoặc Tab Mới)

### Bước 2: Paste URL Này

```
http://localhost:8080/api/payment/vnpay-ipn?vnp_Amount=15900000&vnp_BankCode=NCB&vnp_BankTranNo=VNP15373041&vnp_CardType=ATM&vnp_OrderInfo=Payment_REGULAR&vnp_PayDate=20251229105103&vnp_ResponseCode=00&vnp_TmnCode=NA128BPU&vnp_TransactionNo=15373041&vnp_TransactionStatus=00&vnp_TxnRef=PAY1766980022956050149e1&vnp_SecureHash=3646c7ffd581423bd0583264923d0d9b2b8567184bf609e1a163fffe40f77d095d36e8a84ca2b99edbd7a1907ee6020e718b02ceb75bf7d94b0d9599f0336525
```

### Bước 3: Xem Console Logs

Sau khi paste URL, xem IntelliJ console, phải thấy:

```
VNPay IPN received: {vnp_Amount=15900000, vnp_ResponseCode=00, ...}
VNPay signature verification: received=..., calculated=...
Payment SUCCESS: txnRef=PAY1766980022956050149e1, transactionNo=15373041
Credits added: user=1, chat=100, quiz=0
```

### Bước 4: Kiểm Tra Kết Quả

1. **Xem Response trong Browser**:
   ```json
   {
     "RspCode": "00",
     "Message": "Success"
   }
   ```

2. **Vào Profile Page**:
   - `http://localhost:8080/profile.html`
   - Xem credits balance → phải có **+100 chat credits**

3. **Kiểm Tra Database**:
   ```sql
   SELECT * FROM payment WHERE vnp_txn_ref = 'PAY1766980022956050149e1';
   -- status phải là 'SUCCESS'
   
   SELECT * FROM user_credits WHERE user_id = 1;
   -- chat_credits phải tăng lên 100
   
   SELECT * FROM credit_transactions WHERE user_id = 1 ORDER BY created_at DESC;
   -- phải có transaction mới với type = 'PURCHASE'
   ```

## Tại Sao Cần Test Manual?

VNPay Sandbox có thể **KHÔNG GỌI IPN** cho localhost vì:
- Localhost không thể truy cập từ internet
- VNPay server không thể gọi `http://localhost:8080/api/payment/vnpay-ipn`

## Giải Pháp Production

Khi deploy lên server thật (có domain public), VNPay sẽ gọi IPN tự động.

Hoặc dùng **ngrok** để expose localhost:
```bash
ngrok http 8080
```

Sau đó update `vnpay.ipn-url` trong `application.properties`:
```properties
vnpay.ipn-url=https://your-ngrok-url.ngrok.io/api/payment/vnpay-ipn
```

## Kết Luận

- ✅ Signature đã ĐÚNG (vào được trang VNPay)
- ✅ Payment thành công (vnp_ResponseCode=00)
- ✅ Return URL hoạt động (redirect về payment-result.html)
- ⚠️ IPN callback chưa chạy (do localhost)

**Giải pháp**: Test IPN manual bằng URL ở trên để verify logic cộng credits hoạt động đúng!
