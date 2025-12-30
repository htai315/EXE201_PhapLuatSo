# 🔒 VNPay Security Guide

## ✅ Đã Hoàn Thành

VNPay credentials đã được **BẢO MẬT** và không còn xuất hiện trong code:

### 1. **Credentials được lưu trong `.env`**
```env
VNPAY_TMN_CODE=NA128BPU
VNPAY_HASH_SECRET=WNLMPOIMP9GO2ORARN9CMYVL5F6EA4GU
VNPAY_URL=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
VNPAY_RETURN_URL=http://localhost:8080/html/payment-result.html
VNPAY_IPN_URL=http://localhost:8080/api/payment/vnpay-ipn
```

### 2. **File `.env` đã được gitignore**
✓ File `.env` không bao giờ được commit lên Git
✓ Chỉ có `.env.example` (template) được commit

### 3. **Application.properties chỉ tham chiếu**
```properties
# VNPay Configuration (loaded from environment variables)
vnpay.tmn-code=${VNPAY_TMN_CODE}
vnpay.hash-secret=${VNPAY_HASH_SECRET}
vnpay.url=${VNPAY_URL}
vnpay.return-url=${VNPAY_RETURN_URL}
vnpay.ipn-url=${VNPAY_IPN_URL}
```

### 4. **EnvLoader tự động load từ `.env`**
```java
public static void main(String[] args) {
    // Load .env file before starting Spring Boot
    EnvLoader.loadEnv();
    
    SpringApplication.run(Exe201PhapLuatSoApplication.class, args);
}
```

## 🚀 Cách Sử Dụng

### Development (Local)
1. Copy `.env.example` thành `.env`
2. Điền thông tin VNPay sandbox của bạn
3. Chạy ứng dụng bình thường

### Production
1. Không dùng file `.env` trên production
2. Set environment variables trực tiếp trên server:
   ```bash
   export VNPAY_TMN_CODE=your-production-code
   export VNPAY_HASH_SECRET=your-production-secret
   ```
3. Hoặc dùng Docker secrets, Kubernetes ConfigMap, AWS Parameter Store, etc.

## ⚠️ Lưu Ý Bảo Mật

### ❌ KHÔNG BAO GIỜ:
- Commit file `.env` lên Git
- Share credentials qua email/chat
- Hard-code credentials trong code
- Log credentials ra console (đã được mask trong EnvLoader)

### ✅ NÊN:
- Dùng `.env` cho local development
- Dùng environment variables cho production
- Rotate credentials định kỳ
- Dùng VNPay sandbox cho testing
- Dùng VNPay production credentials riêng cho production

## 🔍 Kiểm Tra

Khi chạy ứng dụng, bạn sẽ thấy log:
```
📁 Loading environment variables from: /path/to/.env
✓ Loaded: VNPAY_TMN_CODE = NA128BPU
✓ Loaded: VNPAY_HASH_SECRET = [HIDDEN]
✅ Successfully loaded 13 environment variables from .env
```

## 📝 Sandbox vs Production

| Environment | TMN Code | Hash Secret | URL |
|------------|----------|-------------|-----|
| **Sandbox** | NA128BPU | WNLMPOIMP9GO2ORARN9CMYVL5F6EA4GU | sandbox.vnpayment.vn |
| **Production** | (Khác) | (Khác) | pay.vnpay.vn |

⚠️ **Lưu ý**: Credentials sandbox và production là KHÁC NHAU!

## 🎯 Best Practices

1. **Local Development**: Dùng `.env` file
2. **CI/CD**: Dùng GitHub Secrets / GitLab CI Variables
3. **Production**: Dùng AWS Secrets Manager / Azure Key Vault
4. **Team**: Share `.env.example`, KHÔNG share `.env`

## 🔗 Tài Liệu Liên Quan

- [VNPay Sandbox](https://sandbox.vnpayment.vn/)
- [VNPay Documentation](https://sandbox.vnpayment.vn/apis/docs/)
- [VNPAY_IMPLEMENTATION_COMPLETE.md](./VNPAY_IMPLEMENTATION_COMPLETE.md)
