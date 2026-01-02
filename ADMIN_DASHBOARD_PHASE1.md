# 🎯 Admin Dashboard - Phase 1: Database & Entities

**Status:** ✅ COMPLETED  
**Date:** December 31, 2024

---

## 📋 OVERVIEW

Phase 1 tạo foundation cho Admin Dashboard bằng cách:
- Thêm fields vào User entity để support ban/unban
- Tạo database migration
- Tạo AdminActivityLog entity để track admin actions
- Tạo indexes để optimize admin queries

---

## ✅ COMPLETED TASKS

### 1. Database Migration
**File:** `src/main/resources/db/migration/V2__add_admin_features.sql`

**Changes:**
- ✅ Added `is_active` field to users table (for ban/unban)
- ✅ Added `ban_reason` field (store reason for banning)
- ✅ Added `banned_at` timestamp
- ✅ Added `banned_by` foreign key (track who banned the user)
- ✅ Created indexes for performance:
  - `ix_users_is_active` - Filter by active status
  - `ix_users_created_at` - Sort by creation date
  - `ix_payments_status_date` - Payment queries
  - `ix_credit_trans_date` - Credit transaction queries
  - `ix_quiz_sets_created_at` - Quiz statistics
  - `ix_chat_sessions_created_at` - Chat statistics
- ✅ Created `payments` table (if not exists)
- ✅ Created `admin_activity_logs` table for audit trail
- ✅ Created `vw_admin_dashboard_stats` view for quick statistics
- ✅ Added admin user seed data (email: admin@phapluatso.vn)

### 2. User Entity Update
**File:** `src/main/java/com/htai/exe201phapluatso/auth/entity/User.java`

**Added Fields:**
```java
@Column(name = "is_active", nullable = false)
private boolean active = true;

@Column(name = "ban_reason", length = 500)
private String banReason;

@Column(name = "banned_at")
private LocalDateTime bannedAt;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "banned_by")
private User bannedBy;
```

**Added Methods:**
- `isActive()` / `setActive()`
- `getBanReason()` / `setBanReason()`
- `getBannedAt()` / `setBannedAt()`
- `getBannedBy()` / `setBannedBy()`

### 3. AdminActivityLog Entity
**File:** `src/main/java/com/htai/exe201phapluatso/admin/entity/AdminActivityLog.java`

**Purpose:** Track all admin actions for audit trail

**Fields:**
- `adminUser` - Who performed the action
- `actionType` - Type of action (BAN_USER, UNBAN_USER, DELETE_USER, etc.)
- `targetType` - Type of target (USER, PAYMENT, DOCUMENT, etc.)
- `targetId` - ID of the target
- `description` - Description of the action
- `ipAddress` - IP address of admin
- `createdAt` - Timestamp

### 4. AdminActivityLog Repository
**File:** `src/main/java/com/htai/exe201phapluatso/admin/repo/AdminActivityLogRepo.java`

**Methods:**
- `findByAdminUserId()` - Get logs by admin user
- `findByActionType()` - Get logs by action type
- `findByTargetTypeAndTargetId()` - Get logs for specific target

---

## 🗄️ DATABASE SCHEMA CHANGES

### Users Table (Updated)
```sql
users
├── id (PK)
├── email
├── password_hash
├── full_name
├── avatar_url
├── provider
├── provider_id
├── email_verified
├── is_enabled
├── created_at
├── is_active          ← NEW (for ban/unban)
├── ban_reason         ← NEW
├── banned_at          ← NEW
└── banned_by (FK)     ← NEW
```

### Admin Activity Logs Table (New)
```sql
admin_activity_logs
├── id (PK)
├── admin_user_id (FK)
├── action_type
├── target_type
├── target_id
├── description
├── ip_address
└── created_at
```

### Payments Table (Created if not exists)
```sql
payments
├── id (PK)
├── user_id (FK)
├── order_id (UNIQUE)
├── plan_code
├── amount
├── status
├── payment_method
├── transaction_no
├── bank_code
├── card_type
├── payment_info
├── created_at
└── paid_at
```

---

## 📊 ADMIN DASHBOARD STATS VIEW

**View:** `vw_admin_dashboard_stats`

**Provides quick access to:**
- Total users, active users, banned users
- New users in last 30 days
- Total successful payments
- Total revenue (all time and last 30 days)
- Total quiz sets and attempts
- Total chat sessions and messages
- Total legal documents and articles

**Usage:**
```sql
SELECT * FROM dbo.vw_admin_dashboard_stats;
```

---

## 🔐 ADMIN USER SEED DATA

**Default Admin Account:**
- Email: `admin@phapluatso.vn`
- Password: **NEEDS TO BE SET MANUALLY**
- Role: ADMIN
- Credits: 999,999 (unlimited)

**⚠️ IMPORTANT:** 
You must set the admin password manually after running the migration:

```sql
UPDATE users 
SET password_hash = '$2a$10$YOUR_BCRYPT_HASH_HERE'
WHERE email = 'admin@phapluatso.vn';
```

Or use the application to register and then update the role to ADMIN.

---

## 🚀 HOW TO RUN MIGRATION

### Option 1: Automatic (Flyway)
When you start the application, Flyway will automatically run the migration:

```bash
mvn spring-boot:run
```

### Option 2: Manual
If you want to run manually:

```bash
# Connect to SQL Server
sqlcmd -S localhost -d phapluatso -U your_username -P your_password

# Run the migration file
:r src/main/resources/db/migration/V2__add_admin_features.sql
GO
```

---

## ✅ VERIFICATION

After running the migration, verify:

### 1. Check new columns exist:
```sql
SELECT COLUMN_NAME, DATA_TYPE 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_NAME = 'users' 
AND COLUMN_NAME IN ('is_active', 'ban_reason', 'banned_at', 'banned_by');
```

### 2. Check indexes created:
```sql
SELECT name, type_desc 
FROM sys.indexes 
WHERE object_id = OBJECT_ID('users')
AND name LIKE 'ix_%';
```

### 3. Check admin_activity_logs table:
```sql
SELECT * FROM INFORMATION_SCHEMA.TABLES 
WHERE TABLE_NAME = 'admin_activity_logs';
```

### 4. Check admin user created:
```sql
SELECT id, email, full_name 
FROM users 
WHERE email = 'admin@phapluatso.vn';
```

### 5. Check view created:
```sql
SELECT * FROM vw_admin_dashboard_stats;
```

---

## 📁 FILES CREATED/MODIFIED

### Created:
- ✅ `src/main/resources/db/migration/V2__add_admin_features.sql`
- ✅ `src/main/java/com/htai/exe201phapluatso/admin/entity/AdminActivityLog.java`
- ✅ `src/main/java/com/htai/exe201phapluatso/admin/repo/AdminActivityLogRepo.java`
- ✅ `ADMIN_DASHBOARD_PHASE1.md` (this file)

### Modified:
- ✅ `src/main/java/com/htai/exe201phapluatso/auth/entity/User.java`

---

## 🎯 NEXT STEPS (Phase 2)

Phase 2 will create DTOs and Response objects:

1. **AdminStatsResponse** - Dashboard statistics
2. **AdminUserListResponse** - User list for admin
3. **AdminUserDetailResponse** - User details
4. **BanUserRequest** - Request to ban user
5. **AdminPaymentListResponse** - Payment list
6. **AdminPaymentStatsResponse** - Payment statistics
7. **RevenueByDate** - Revenue chart data
8. **UserGrowth** - User growth chart data

---

## 📝 NOTES

- All admin fields have default values to ensure backward compatibility
- Indexes are created to optimize admin queries
- AdminActivityLog provides full audit trail
- The view `vw_admin_dashboard_stats` provides quick access to statistics without complex queries
- Admin user is created with unlimited credits

---

## 🐛 TROUBLESHOOTING

### Migration fails with "Column already exists"
If you've run the migration before, you may need to rollback:

```sql
-- Check Flyway schema history
SELECT * FROM flyway_schema_history;

-- If V2 exists and failed, delete it
DELETE FROM flyway_schema_history WHERE version = '2';

-- Then run migration again
```

### Admin user not created
Check if the user already exists:

```sql
SELECT * FROM users WHERE email = 'admin@phapluatso.vn';
```

If not, run the INSERT statement manually from the migration file.

---

**Phase 1 Status:** ✅ COMPLETED  
**Ready for Phase 2:** ✅ YES

