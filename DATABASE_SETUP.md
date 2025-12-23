# Database Setup - Credits Model

## Tạo Database Mới

Mở **SQL Server Management Studio** và chạy:

```sql
CREATE DATABASE EXE201_PhapLuatSo;
```

## Chạy Application

```bash
.\mvnw.cmd spring-boot:run
```

Application sẽ tự động:
- Chạy migration `V1__init_clean.sql`
- Tạo 18 tables
- Tạo 3 plans: FREE (10 chat), REGULAR (100 chat - 159K), STUDENT (100 chat + 20 quiz - 249K)
- Tạo 2 roles: USER, ADMIN
- Tạo triggers tự động cấp 10 lượt FREE cho user mới

## Verify

```sql
USE EXE201_PhapLuatSo;

-- Check tables
SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'dbo';

-- Check plans
SELECT * FROM plans;

-- Check roles
SELECT * FROM roles;
```

## Model: Pay-per-use Credits

- **FREE**: 10 lượt chat (dùng thử duy nhất) - 0 VND
- **REGULAR**: 100 lượt chat - 159,000 VND (hạn 12 tháng)
- **STUDENT**: 100 lượt chat + 20 lượt AI tạo đề - 249,000 VND (hạn 12 tháng)

User đăng ký tự động nhận 10 lượt FREE.
