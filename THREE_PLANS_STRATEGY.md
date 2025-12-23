# Three Plans Strategy - FREE, REGULAR, STUDENT

## Overview

Hệ thống có **3 gói dịch vụ** phục vụ 3 nhóm người dùng khác nhau:

1. **FREE** (0 VND) - Người dùng thử nghiệm
2. **REGULAR** (149,000 VND/năm) - Dân thường (general public)
3. **STUDENT** (99,000 VND/năm) - Sinh viên (student discount)

---

## Plans Comparison

| Feature | FREE | REGULAR | STUDENT |
|---------|------|---------|---------|
| **Giá** | 0 VND | 149,000 VND/năm | 99,000 VND/năm |
| **Đối tượng** | Người dùng thử | Dân thường | Sinh viên |
| **Xác thực** | Không | Không | Cần MSSV/thẻ SV |

### Chat AI

| Feature | FREE | REGULAR | STUDENT |
|---------|------|---------|---------|
| Số câu hỏi/ngày | 5 | Unlimited | Unlimited |
| Lịch sử chat | 3 ngày | Unlimited | Unlimited |
| AI re-ranking | ❌ | ✅ | ✅ |
| Context memory | ❌ | ✅ | ✅ |
| Độ ưu tiên | Thấp | Cao | Cao |

### Quiz System

| Feature | FREE | REGULAR | STUDENT |
|---------|------|---------|---------|
| Làm quiz | ✅ (3 lần/ngày) | ✅ Unlimited | ✅ Unlimited |
| Tạo quiz set | ❌ | ✅ | ✅ |
| AI tạo quiz | ❌ | ✅ (10 bộ/tháng) | ✅ (20 bộ/tháng) |
| Số quiz set | 0 | Unlimited | Unlimited |
| Export kết quả | ❌ | ✅ | ✅ |

### Legal Documents

| Feature | FREE | REGULAR | STUDENT |
|---------|------|---------|---------|
| Xem văn bản | ✅ | ✅ | ✅ |
| Upload văn bản | ❌ | ✅ (50 MB) | ✅ (100 MB) |
| Tìm kiếm nâng cao | ❌ | ✅ | ✅ |
| Bookmark | 10 | Unlimited | Unlimited |

### Other Features

| Feature | FREE | REGULAR | STUDENT |
|---------|------|---------|---------|
| Quảng cáo | Có | Không | Không |
| Hỗ trợ | Community | Email (48h) | Email (24h) |
| API access | ❌ | ❌ | ❌ |
| Ưu tiên server | Thấp | Cao | Cao |

---

## Database Schema

### Plans Table

```sql
CREATE TABLE dbo.plans (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    code NVARCHAR(50) NOT NULL UNIQUE,   -- FREE, REGULAR, STUDENT
    name NVARCHAR(100) NOT NULL,
    price INT NOT NULL DEFAULT 0,
    duration_months INT NOT NULL DEFAULT 12,  -- Subscription duration
    description NVARCHAR(500) NULL,
    features NVARCHAR(MAX) NULL,  -- JSON string of features
    is_active BIT NOT NULL DEFAULT 1,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
);
```

### Seed Data (V10 Migration)

```sql
-- Existing plans (from V1)
INSERT INTO dbo.plans(code, name, price) VALUES
    (N'FREE', N'Free', 0),
    (N'STUDENT', N'Student', 99000);

-- New plan (V10)
INSERT INTO dbo.plans(code, name, price) VALUES
    (N'REGULAR', N'Dân Thường', 149000);
```

### Final Plans in Database

| ID | Code | Name | Price (VND) |
|----|------|------|-------------|
| 1 | FREE | Free | 0 |
| 2 | STUDENT | Student | 99,000 |
| 3 | REGULAR | Dân Thường | 149,000 |

---

## Pricing Strategy

### Why 3 Plans?

1. **FREE** - Acquisition funnel
   - Cho người dùng thử nghiệm
   - Convert sang paid plans
   - Viral marketing

2. **STUDENT** - Student segment
   - Giá ưu đãi cho sinh viên
   - Xây dựng thói quen sử dụng
   - Future customers (sau khi tốt nghiệp)

3. **REGULAR** - Main revenue
   - Giá đầy đủ cho dân thường
   - Target: Luật sư, công chức, doanh nghiệp
   - Highest profit margin

### Price Points

```
FREE: 0 VND
    ↓ (33% discount)
STUDENT: 99,000 VND/năm (~8,250 VND/tháng)
    ↓ (50% premium)
REGULAR: 149,000 VND/năm (~12,400 VND/tháng)
```

**Rationale:**
- STUDENT: Affordable for students (~8K/month = 1 ly trà sữa)
- REGULAR: Reasonable for professionals (~12K/month = 1 bữa cơm)
- Gap: 50K difference encourages student verification

---

## Student Verification

### Required Documents

1. **Thẻ sinh viên** (Student ID card)
2. **MSSV** (Student code)
3. **Email sinh viên** (@student.hcmus.edu.vn, @sv.uit.edu.vn, etc.)

### Verification Flow

```
User selects STUDENT plan
    ↓
Upload thẻ sinh viên photo
    ↓
Enter MSSV
    ↓
Admin reviews (manual or auto)
    ↓
Approved → Create STUDENT subscription
    ↓
Rejected → Suggest REGULAR plan
```

### Verification Table

```sql
CREATE TABLE dbo.student_verifications (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    student_id NVARCHAR(50) NOT NULL,  -- MSSV
    university NVARCHAR(200) NULL,
    student_card_url NVARCHAR(500) NULL,  -- Photo of student card
    status NVARCHAR(20) NOT NULL DEFAULT 'PENDING',  -- PENDING, APPROVED, REJECTED
    verified_by BIGINT NULL,  -- Admin user ID
    verified_at DATETIME2 NULL,
    rejection_reason NVARCHAR(500) NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_sv_user FOREIGN KEY (user_id) REFERENCES dbo.users(id),
    CONSTRAINT fk_sv_admin FOREIGN KEY (verified_by) REFERENCES dbo.users(id),
    CONSTRAINT ck_sv_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
);

CREATE INDEX ix_sv_user ON dbo.student_verifications(user_id);
CREATE INDEX ix_sv_status ON dbo.student_verifications(status);
```

---

## Feature Access Control

### Service Layer Implementation

```java
@Service
public class PlanService {
    
    public enum PlanCode {
        FREE, REGULAR, STUDENT
    }
    
    public PlanCode getUserPlan(Long userId) {
        Subscription sub = subscriptionRepo
            .findActiveByUserId(userId)
            .orElse(null);
        
        if (sub == null) {
            return PlanCode.FREE;
        }
        
        return PlanCode.valueOf(sub.getPlan().getCode());
    }
    
    public boolean canCreateQuizSet(Long userId) {
        PlanCode plan = getUserPlan(userId);
        return plan == PlanCode.REGULAR || plan == PlanCode.STUDENT;
    }
    
    public boolean canUseAIQuizGeneration(Long userId) {
        PlanCode plan = getUserPlan(userId);
        return plan == PlanCode.REGULAR || plan == PlanCode.STUDENT;
    }
    
    public int getAIQuizGenerationLimit(Long userId) {
        PlanCode plan = getUserPlan(userId);
        return switch (plan) {
            case FREE -> 0;
            case REGULAR -> 10;  // 10 per month
            case STUDENT -> 20;  // 20 per month
        };
    }
    
    public int getDailyChatLimit(Long userId) {
        PlanCode plan = getUserPlan(userId);
        return switch (plan) {
            case FREE -> 5;
            case REGULAR, STUDENT -> Integer.MAX_VALUE;  // Unlimited
        };
    }
    
    public void requirePaidPlan(Long userId) {
        PlanCode plan = getUserPlan(userId);
        if (plan == PlanCode.FREE) {
            throw new ForbiddenException(
                "This feature requires REGULAR or STUDENT plan. Please upgrade."
            );
        }
    }
}
```

### Usage in Services

```java
@Service
public class QuizService {
    
    private final PlanService planService;
    
    public QuizSet createQuizSet(Long userId, CreateQuizSetRequest request) {
        // Check plan
        planService.requirePaidPlan(userId);
        
        // Create quiz set
        // ...
    }
}

@Service
public class AIQuizService {
    
    private final PlanService planService;
    
    public QuizSet generateQuiz(Long userId, GenerateQuizRequest request) {
        // Check plan
        planService.requirePaidPlan(userId);
        
        // Check monthly limit
        int limit = planService.getAIQuizGenerationLimit(userId);
        int used = usageService.getMonthlyAIQuizCount(userId);
        
        if (used >= limit) {
            throw new ForbiddenException(
                "You have reached your monthly AI quiz generation limit (" + limit + ")"
            );
        }
        
        // Generate quiz
        // ...
    }
}

@Service
public class LegalChatService {
    
    private final PlanService planService;
    
    public ChatResponse chat(Long userId, String question) {
        // Check daily limit
        int limit = planService.getDailyChatLimit(userId);
        int used = usageService.getDailyChatCount(userId);
        
        if (used >= limit) {
            throw new ForbiddenException(
                "You have reached your daily chat limit (" + limit + "). " +
                "Upgrade to REGULAR or STUDENT plan for unlimited access."
            );
        }
        
        // Process chat
        // ...
    }
}
```

---

## Usage Tracking

### Usage Logs Table

```sql
CREATE TABLE dbo.usage_logs (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    feature NVARCHAR(50) NOT NULL,  -- CHAT_AI, QUIZ_ATTEMPT, AI_QUIZ_GEN, etc.
    plan_code NVARCHAR(50) NOT NULL,  -- Plan at time of usage
    metadata NVARCHAR(MAX) NULL,  -- JSON for additional data
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_usage_user FOREIGN KEY (user_id) REFERENCES dbo.users(id)
);

CREATE INDEX ix_usage_user_feature_date 
ON dbo.usage_logs(user_id, feature, created_at DESC);

CREATE INDEX ix_usage_date_feature 
ON dbo.usage_logs(created_at DESC, feature);
```

### Usage Tracking Service

```java
@Service
public class UsageService {
    
    public void logUsage(Long userId, String feature, String planCode) {
        UsageLog log = new UsageLog();
        log.setUserId(userId);
        log.setFeature(feature);
        log.setPlanCode(planCode);
        log.setCreatedAt(LocalDateTime.now());
        usageLogRepo.save(log);
    }
    
    public int getDailyChatCount(Long userId) {
        LocalDateTime startOfDay = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS);
        return usageLogRepo.countByUserIdAndFeatureAndCreatedAtAfter(
            userId, "CHAT_AI", startOfDay
        );
    }
    
    public int getMonthlyAIQuizCount(Long userId) {
        LocalDateTime startOfMonth = LocalDateTime.now()
            .withDayOfMonth(1)
            .truncatedTo(ChronoUnit.DAYS);
        return usageLogRepo.countByUserIdAndFeatureAndCreatedAtAfter(
            userId, "AI_QUIZ_GEN", startOfMonth
        );
    }
}
```

---

## Plan Upgrade/Downgrade Flow

### Upgrade Flow

```
FREE user wants to upgrade
    ↓
Choose plan: REGULAR or STUDENT
    ↓
If STUDENT → Verify student status
    ↓
Proceed to payment
    ↓
Payment successful
    ↓
Create subscription (ACTIVE)
    ↓
Send confirmation email
    ↓
Unlock features
```

### Downgrade Flow

```
REGULAR/STUDENT subscription expires
    ↓
Scheduled job checks expiry
    ↓
Update subscription status to EXPIRED
    ↓
Create new FREE subscription
    ↓
Send expiry notification
    ↓
Lock premium features
```

### Plan Change Rules

| From | To | Action |
|------|----|----|
| FREE | REGULAR | Payment required |
| FREE | STUDENT | Verification + Payment |
| REGULAR | STUDENT | Refund difference + Verification |
| STUDENT | REGULAR | Pay difference |
| REGULAR | FREE | Cancel subscription |
| STUDENT | FREE | Cancel subscription |

---

## UI/UX Considerations

### Plan Selection Page

```html
<div class="plans-container">
    <!-- FREE Plan -->
    <div class="plan-card free">
        <h3>FREE</h3>
        <div class="price">0 VND</div>
        <ul class="features">
            <li>✅ 5 câu hỏi AI/ngày</li>
            <li>✅ Làm quiz (3 lần/ngày)</li>
            <li>✅ Xem văn bản pháp luật</li>
            <li>❌ Tạo quiz set</li>
            <li>❌ AI tạo quiz</li>
        </ul>
        <button class="btn-select" disabled>Current Plan</button>
    </div>
    
    <!-- STUDENT Plan -->
    <div class="plan-card student popular">
        <div class="badge">Phổ biến nhất</div>
        <h3>STUDENT</h3>
        <div class="price">99,000 VND<span>/năm</span></div>
        <div class="discount">Giảm 33% cho sinh viên</div>
        <ul class="features">
            <li>✅ Chat AI không giới hạn</li>
            <li>✅ Tạo quiz set không giới hạn</li>
            <li>✅ AI tạo quiz (20 bộ/tháng)</li>
            <li>✅ Upload văn bản (100 MB)</li>
            <li>✅ Không quảng cáo</li>
        </ul>
        <button class="btn-select btn-primary">
            Chọn gói STUDENT
        </button>
        <small>* Cần xác thực thẻ sinh viên</small>
    </div>
    
    <!-- REGULAR Plan -->
    <div class="plan-card regular">
        <h3>DÂN THƯỜNG</h3>
        <div class="price">149,000 VND<span>/năm</span></div>
        <ul class="features">
            <li>✅ Chat AI không giới hạn</li>
            <li>✅ Tạo quiz set không giới hạn</li>
            <li>✅ AI tạo quiz (10 bộ/tháng)</li>
            <li>✅ Upload văn bản (50 MB)</li>
            <li>✅ Không quảng cáo</li>
        </ul>
        <button class="btn-select">
            Chọn gói DÂN THƯỜNG
        </button>
    </div>
</div>
```

### Plan Badge in Navbar

```html
<div class="user-plan-badge">
    <span class="badge badge-free">FREE</span>
    <!-- or -->
    <span class="badge badge-student">STUDENT</span>
    <!-- or -->
    <span class="badge badge-regular">REGULAR</span>
</div>
```

### Upgrade Prompt

```html
<div class="upgrade-prompt">
    <i class="bi bi-lock"></i>
    <h4>Tính năng Premium</h4>
    <p>Tính năng này yêu cầu gói REGULAR hoặc STUDENT</p>
    <button class="btn btn-primary">Nâng cấp ngay</button>
</div>
```

---

## Marketing Strategy

### Target Audience

1. **FREE → STUDENT**
   - Target: Sinh viên luật, kinh tế, quản trị
   - Message: "Chỉ 8,250 VND/tháng = 1 ly trà sữa"
   - Channel: Facebook groups, university forums

2. **FREE → REGULAR**
   - Target: Luật sư, công chức, doanh nghiệp
   - Message: "Tiết kiệm thời gian, nâng cao hiệu quả"
   - Channel: LinkedIn, legal forums

3. **STUDENT → REGULAR**
   - Target: Sinh viên sắp tốt nghiệp
   - Message: "Tiếp tục sử dụng sau khi tốt nghiệp"
   - Offer: Discount for first year after graduation

### Conversion Tactics

1. **Free Trial Extended**
   - Give FREE users 7-day STUDENT trial
   - Show value of premium features

2. **Usage Limits**
   - Show "3/5 daily chats used" counter
   - Prompt upgrade when limit reached

3. **Social Proof**
   - "1,000+ sinh viên đã nâng cấp"
   - Testimonials from users

4. **Urgency**
   - "Ưu đãi kết thúc sau 3 ngày"
   - Limited-time discounts

---

## Revenue Projections

### Assumptions

- Total users: 10,000
- FREE: 70% (7,000 users)
- STUDENT: 20% (2,000 users)
- REGULAR: 10% (1,000 users)

### Annual Revenue

```
STUDENT: 2,000 × 99,000 = 198,000,000 VND
REGULAR: 1,000 × 149,000 = 149,000,000 VND
TOTAL: 347,000,000 VND (~$14,000 USD)
```

### Costs

```
OpenAI API: ~50,000,000 VND/year
Server: ~20,000,000 VND/year
Marketing: ~30,000,000 VND/year
TOTAL COSTS: ~100,000,000 VND/year
```

### Profit

```
Revenue: 347,000,000 VND
Costs: 100,000,000 VND
PROFIT: 247,000,000 VND (~$10,000 USD)
Margin: 71%
```

---

## Implementation Checklist

### Phase 1: Database (✅ Done)
- [x] Create V10 migration
- [x] Add REGULAR plan
- [x] Test migration

### Phase 2: Backend (Next)
- [ ] Create PlanService
- [ ] Add plan checking in services
- [ ] Create UsageService
- [ ] Add usage tracking
- [ ] Create student verification endpoints

### Phase 3: Frontend (After Phase 2)
- [ ] Create plans comparison page
- [ ] Add upgrade flow
- [ ] Add student verification form
- [ ] Show plan badge in UI
- [ ] Add usage counters

### Phase 4: Payment (After Phase 3)
- [ ] Integrate VNPay/Momo
- [ ] Create payment endpoints
- [ ] Handle payment callbacks
- [ ] Send confirmation emails

### Phase 5: Admin (After Phase 4)
- [ ] Admin dashboard for plans
- [ ] Student verification review
- [ ] Usage analytics
- [ ] Revenue reports

---

## Summary

### 3 Plans Structure

| Plan | Price | Target | Key Benefit |
|------|-------|--------|-------------|
| **FREE** | 0 VND | Trial users | Try before buy |
| **STUDENT** | 99,000 VND | Students | Affordable + Full features |
| **REGULAR** | 149,000 VND | General public | Full features |

### Key Differences

- **Price**: STUDENT 33% cheaper than REGULAR
- **Verification**: STUDENT requires student ID
- **Features**: STUDENT gets more AI quota (20 vs 10)
- **Target**: Different market segments

### Next Steps

1. ✅ Run V10 migration to add REGULAR plan
2. Implement PlanService for access control
3. Add student verification system
4. Create plans comparison UI
5. Integrate payment gateway

---

**Last Updated**: December 23, 2024
**Migration**: V10 (Add REGULAR plan)
**Plans**: FREE (0 VND), STUDENT (99K VND), REGULAR (149K VND)
