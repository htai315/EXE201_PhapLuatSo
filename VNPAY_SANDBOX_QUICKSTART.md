# VNPay Sandbox - Kế Hoạch Test Miễn Phí

## Bước 0: Đăng Ký Sandbox (5 phút)

### Truy cập và đăng ký
1. Vào: https://sandbox.vnpayment.vn/devreg/
2. Điền form đăng ký (email, tên, SĐT)
3. Nhận email xác nhận
4. Login vào dashboard

### Nhận credentials
Sau khi login, bạn sẽ nhận được:
```
TMN Code: (ví dụ: DEMOV210)
Hash Secret: (ví dụ: RAOEXHYVSDDIIENYWSLDIIZTANXUXZFJ)
```

**LƯU Ý**: Credentials này chỉ dùng để test, không nhận tiền thật!

---

## Bước 1: Database Migration (10 phút)

### Tạo file migration
**File**: `src/main/resources/db/migration/V2__add_payment_tables.sql`

```sql
-- Payment transactions table
CREATE TABLE payments (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    plan_code VARCHAR(20) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    
    -- VNPay fields
    vnp_txn_ref VARCHAR(100) UNIQUE NOT NULL,
    vnp_transaction_no VARCHAR(100),
    vnp_bank_code VARCHAR(20),
    vnp_card_type VARCHAR(20),
    
    -- Status
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payment_method VARCHAR(20) DEFAULT 'VNPAY',
    
    -- Timestamps
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    paid_at DATETIME2,
    
    -- Metadata
    ip_address VARCHAR(50),
    
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (plan_code) REFERENCES plans(code)
);

CREATE INDEX idx_payments_user_id ON payments(user_id);
CREATE INDEX idx_payments_vnp_txn_ref ON payments(vnp_txn_ref);
CREATE INDEX idx_payments_status ON payments(status);
```

### Chạy migration
```bash
./mvnw flyway:migrate
```

---

## Bước 2: Add Dependencies (2 phút)

**File**: `pom.xml`

Thêm vào `<dependencies>`:
```xml
<!-- For HMAC SHA512 signature -->
<dependency>
    <groupId>commons-codec</groupId>
    <artifactId>commons-codec</artifactId>
</dependency>
```

Sau đó:
```bash
./mvnw clean install
```

---

## Bước 3: Configuration (5 phút)

**File**: `src/main/resources/application.properties`

Thêm vào cuối file:
```properties
# VNPay Sandbox Configuration
vnpay.tmn-code=DEMOV210
vnpay.hash-secret=RAOEXHYVSDDIIENYWSLDIIZTANXUXZFJ
vnpay.url=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
vnpay.return-url=http://localhost:8080/payment-result.html
vnpay.ipn-url=http://localhost:8080/api/payment/vnpay-ipn
```

**QUAN TRỌNG**: Thay `DEMOV210` và `RAOEXHYVSDDIIENYWSLDIIZTANXUXZFJ` bằng credentials bạn nhận được!

---

## Bước 4: Tạo Entity & Repository (10 phút)

Tôi sẽ tạo các file cần thiết. Bạn muốn tôi:
1. **Tạo tất cả files ngay** (recommend)
2. **Giải thích từng file** trước khi tạo

Chọn 1 hoặc 2?
