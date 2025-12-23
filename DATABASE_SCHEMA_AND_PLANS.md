# Database Schema & Plans Analysis

## Current Database Schema

### Core Tables Overview

```
users (authentication & profile)
  ├── roles (USER, ADMIN)
  ├── plans (FREE, STUDENT)
  ├── subscriptions (user plan assignments)
  └── refresh_tokens (JWT refresh tokens)

quiz_sets (quiz management)
  ├── quiz_questions
  ├── quiz_question_options
  ├── quiz_attempts
  └── quiz_attempt_answers

legal_documents (legal content)
  └── legal_articles (searchable articles)

chat_sessions (chat history)
  ├── chat_messages
  └── chat_message_citations
```

---

## Plans System

### Current Plans (from V1__init.sql)

| Plan Code | Plan Name | Price (VND) | Description |
|-----------|-----------|-------------|-------------|
| **FREE** | Free | 0 | Dân thường (general public) |
| **STUDENT** | Student | 99,000 | Sinh viên (students) |

### Plans Table Schema

```sql
CREATE TABLE dbo.plans (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    code NVARCHAR(50) NOT NULL UNIQUE,   -- FREE, STUDENT
    name NVARCHAR(100) NOT NULL,
    price INT NOT NULL DEFAULT 0
);
```

**Seed Data:**
```sql
INSERT INTO dbo.plans(code, name, price) VALUES
    (N'FREE', N'Free', 0),
    (N'STUDENT', N'Student', 99000);
```

---

## Subscriptions System

### Subscriptions Table Schema

```sql
CREATE TABLE dbo.subscriptions (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    plan_id BIGINT NOT NULL,
    status NVARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    start_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    end_at DATETIME2 NULL,
    CONSTRAINT fk_sub_user FOREIGN KEY (user_id) REFERENCES dbo.users(id),
    CONSTRAINT fk_sub_plan FOREIGN KEY (plan_id) REFERENCES dbo.plans(id)
);
```

### Subscription Status

| Status | Description |
|--------|-------------|
| **ACTIVE** | Subscription is currently active |
| **EXPIRED** | Subscription has ended (end_at < now) |
| **CANCELLED** | User cancelled subscription |

### Subscription Flow

```
User Registration
    ↓
Assign FREE plan (default)
    ↓
User upgrades to STUDENT
    ↓
Create new subscription (ACTIVE)
    ↓
Set end_at (e.g., 1 year from now)
    ↓
Subscription expires
    ↓
Status changes to EXPIRED
    ↓
Downgrade to FREE plan
```

---

## Feature Access by Plan

### FREE Plan (0 VND)

**Allowed:**
- ✅ View legal documents
- ✅ Basic chat AI (limited queries per day?)
- ✅ View public quiz sets
- ✅ Take quizzes (limited attempts?)

**Restricted:**
- ❌ Create custom quiz sets
- ❌ AI quiz generation
- ❌ Unlimited chat AI queries
- ❌ Advanced features

### STUDENT Plan (99,000 VND)

**Allowed:**
- ✅ All FREE features
- ✅ Create unlimited quiz sets
- ✅ AI quiz generation
- ✅ Unlimited chat AI queries
- ✅ Upload legal documents
- ✅ Chat history (unlimited)
- ✅ Advanced analytics

**Premium Features:**
- ✅ Priority support
- ✅ No ads (if any)
- ✅ Export quiz results
- ✅ Collaboration features

---

## Current Implementation Status

### ✅ Implemented

1. **Plans Table** - FREE and STUDENT plans defined
2. **Subscriptions Table** - User plan assignments
3. **User Authentication** - Email/password and Google OAuth
4. **Role System** - USER and ADMIN roles
5. **Quiz System** - Full CRUD with attempts tracking
6. **Legal Documents** - Upload and search
7. **Chat History** - Sessions and messages with citations

### ⚠️ Partially Implemented

1. **Plan Enforcement** - Need to check plan before allowing features
2. **Subscription Management** - No UI for upgrading/downgrading
3. **Payment Integration** - No payment gateway (VNPay, Momo, etc.)
4. **Usage Limits** - No rate limiting based on plan

### ❌ Not Implemented

1. **Plan Comparison Page** - Show FREE vs STUDENT features
2. **Upgrade Flow** - UI for users to upgrade to STUDENT
3. **Payment Processing** - Integration with payment gateway
4. **Subscription Expiry** - Auto-downgrade when subscription expires
5. **Usage Analytics** - Track feature usage per plan
6. **Admin Dashboard** - Manage plans and subscriptions

---

## Recommended Plan Features

### Feature Matrix

| Feature | FREE | STUDENT |
|---------|------|---------|
| **Chat AI** |
| Daily queries | 10 | Unlimited |
| Chat history | 7 days | Unlimited |
| AI re-ranking | ❌ | ✅ |
| Context memory | ❌ | ✅ |
| **Quiz** |
| Take quizzes | ✅ | ✅ |
| Create quiz sets | ❌ | ✅ |
| AI generation | ❌ | ✅ |
| Quiz attempts | 3/day | Unlimited |
| **Legal Documents** |
| View documents | ✅ | ✅ |
| Upload documents | ❌ | ✅ |
| Advanced search | ❌ | ✅ |
| **Other** |
| Ads | Yes | No |
| Support | Community | Priority |
| Export data | ❌ | ✅ |

---

## Implementation Roadmap

### Phase 1: Plan Enforcement (1-2 days)

**Goal**: Restrict features based on user's plan

**Tasks:**
1. Create `PlanService` to check user's active plan
2. Add `@RequiresPlan("STUDENT")` annotation
3. Implement plan checking in services:
   - `QuizService.createQuizSet()` - Require STUDENT
   - `AIQuizService.generateQuiz()` - Require STUDENT
   - `LegalDocumentService.uploadDocument()` - Require STUDENT
   - `ChatHistoryService` - Limit FREE users
4. Add plan info to user profile API
5. Show plan badge in UI

**Example Code:**
```java
@Service
public class PlanService {
    public boolean hasActivePlan(Long userId, String planCode) {
        return subscriptionRepo.existsByUserIdAndPlanCodeAndStatus(
            userId, planCode, "ACTIVE"
        );
    }
    
    public void requirePlan(Long userId, String planCode) {
        if (!hasActivePlan(userId, planCode)) {
            throw new ForbiddenException("This feature requires " + planCode + " plan");
        }
    }
}
```

### Phase 2: Subscription Management UI (2-3 days)

**Goal**: Allow users to view and upgrade plans

**Tasks:**
1. Create `/html/plans.html` - Plan comparison page
2. Create `/html/upgrade.html` - Upgrade flow
3. Add "Upgrade to STUDENT" button in navbar for FREE users
4. Show current plan in profile page
5. Add plan expiry countdown for STUDENT users

**UI Components:**
- Plan comparison cards
- Feature checklist
- Pricing display
- "Upgrade Now" button
- Success/error messages

### Phase 3: Payment Integration (3-5 days)

**Goal**: Process payments for STUDENT plan

**Tasks:**
1. Choose payment gateway (VNPay, Momo, ZaloPay)
2. Implement payment API endpoints
3. Create payment callback handler
4. Update subscription on successful payment
5. Send confirmation email
6. Add payment history page

**Payment Flow:**
```
User clicks "Upgrade to STUDENT"
    ↓
Redirect to payment gateway
    ↓
User completes payment
    ↓
Payment gateway callback
    ↓
Verify payment signature
    ↓
Create/update subscription
    ↓
Send confirmation email
    ↓
Redirect to success page
```

### Phase 4: Usage Limits & Analytics (2-3 days)

**Goal**: Track and limit feature usage

**Tasks:**
1. Create `usage_logs` table
2. Implement rate limiting:
   - Chat AI: 10 queries/day for FREE
   - Quiz attempts: 3/day for FREE
3. Add usage counters in UI
4. Create admin analytics dashboard
5. Send usage alerts

**Usage Logs Table:**
```sql
CREATE TABLE dbo.usage_logs (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    feature NVARCHAR(50) NOT NULL, -- CHAT_AI, QUIZ_ATTEMPT, etc.
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_usage_user FOREIGN KEY (user_id) REFERENCES dbo.users(id)
);

CREATE INDEX ix_usage_user_feature_date 
ON dbo.usage_logs(user_id, feature, created_at DESC);
```

### Phase 5: Subscription Expiry (1-2 days)

**Goal**: Auto-downgrade expired subscriptions

**Tasks:**
1. Create scheduled job to check expiry
2. Update subscription status to EXPIRED
3. Create new FREE subscription
4. Send expiry notification email
5. Show renewal reminder in UI

**Scheduled Job:**
```java
@Scheduled(cron = "0 0 0 * * *") // Daily at midnight
public void checkExpiredSubscriptions() {
    List<Subscription> expired = subscriptionRepo
        .findByStatusAndEndAtBefore("ACTIVE", LocalDateTime.now());
    
    for (Subscription sub : expired) {
        sub.setStatus("EXPIRED");
        subscriptionRepo.save(sub);
        
        // Create FREE subscription
        createFreeSubscription(sub.getUserId());
        
        // Send email
        emailService.sendExpiryNotification(sub.getUser());
    }
}
```

---

## Database Optimization

### Recommended Indexes

```sql
-- Subscriptions
CREATE INDEX ix_subscriptions_user_status 
ON dbo.subscriptions(user_id, status);

CREATE INDEX ix_subscriptions_end_at 
ON dbo.subscriptions(end_at) 
WHERE status = 'ACTIVE';

-- Usage logs (if implemented)
CREATE INDEX ix_usage_user_feature_date 
ON dbo.usage_logs(user_id, feature, created_at DESC);
```

### Query Optimization

**Check Active Plan:**
```sql
-- Current (slow)
SELECT * FROM subscriptions 
WHERE user_id = ? AND status = 'ACTIVE';

-- Optimized (with index)
SELECT plan_id FROM subscriptions 
WHERE user_id = ? AND status = 'ACTIVE' 
AND (end_at IS NULL OR end_at > GETDATE())
LIMIT 1;
```

---

## Security Considerations

### 1. Plan Verification
- Always verify plan on server-side
- Never trust client-side plan checks
- Cache plan info with short TTL (5 minutes)

### 2. Payment Security
- Use HTTPS for all payment requests
- Verify payment gateway signatures
- Log all payment transactions
- Implement idempotency for payments

### 3. Subscription Tampering
- Prevent users from manually updating subscriptions
- Add audit logs for subscription changes
- Require admin approval for manual upgrades

---

## Testing Checklist

### Unit Tests
- [ ] PlanService.hasActivePlan()
- [ ] PlanService.requirePlan()
- [ ] SubscriptionService.createSubscription()
- [ ] SubscriptionService.expireSubscription()

### Integration Tests
- [ ] User with FREE plan cannot create quiz sets
- [ ] User with STUDENT plan can create quiz sets
- [ ] Expired subscription downgrades to FREE
- [ ] Payment callback creates subscription

### E2E Tests
- [ ] User can view plan comparison
- [ ] User can upgrade to STUDENT
- [ ] Payment flow works end-to-end
- [ ] Subscription expiry works correctly

---

## Cost Analysis

### STUDENT Plan Pricing

**Current**: 99,000 VND/year

**Breakdown:**
- OpenAI API costs: ~20,000 VND/year (estimated)
- Server costs: ~10,000 VND/year (per user)
- Profit margin: ~69,000 VND/year (70%)

**Recommendations:**
- Consider monthly option: 9,900 VND/month
- Add 6-month option: 54,000 VND (10% discount)
- Student verification for discount

---

## Future Plans (Optional)

### PREMIUM Plan (199,000 VND/year)

**Additional Features:**
- API access
- White-label option
- Custom branding
- Team collaboration (5 users)
- Advanced analytics
- Priority support (24/7)

### ENTERPRISE Plan (Custom pricing)

**Features:**
- Unlimited users
- On-premise deployment
- Custom integrations
- Dedicated support
- SLA guarantee
- Training sessions

---

## Summary

### Current State
- ✅ Database schema supports plans and subscriptions
- ✅ FREE and STUDENT plans defined
- ⚠️ No plan enforcement in code
- ❌ No payment integration
- ❌ No subscription management UI

### Next Steps
1. **Immediate**: Implement plan enforcement (Phase 1)
2. **Short-term**: Add subscription management UI (Phase 2)
3. **Medium-term**: Integrate payment gateway (Phase 3)
4. **Long-term**: Add usage limits and analytics (Phase 4-5)

### Estimated Timeline
- Phase 1: 1-2 days
- Phase 2: 2-3 days
- Phase 3: 3-5 days
- Phase 4: 2-3 days
- Phase 5: 1-2 days

**Total**: 9-15 days for complete implementation

---

**Last Updated**: December 23, 2024
**Database Version**: V9 (Chat History)
**Plans**: FREE (0 VND), STUDENT (99,000 VND)
