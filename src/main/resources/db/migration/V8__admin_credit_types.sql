-- V8: Add ADMIN_ADD, ADMIN_REMOVE, RESERVE, CONFIRM to credit_transactions type constraint
-- This allows admin to add/remove credits and credit reservation system to work properly

-- Drop old constraint and add new one with admin types and reservation types
ALTER TABLE credit_transactions DROP CONSTRAINT IF EXISTS ck_trans_type;

ALTER TABLE credit_transactions ADD CONSTRAINT ck_trans_type 
    CHECK (type IN ('PURCHASE', 'USAGE', 'BONUS', 'REFUND', 'EXPIRE', 'ADMIN_ADD', 'ADMIN_REMOVE', 'RESERVE', 'CONFIRM'));

-- Also add ADD_CREDITS and REMOVE_CREDITS to admin_activity_logs action_type
ALTER TABLE admin_activity_logs DROP CONSTRAINT IF EXISTS ck_action_type;

ALTER TABLE admin_activity_logs ADD CONSTRAINT ck_action_type 
    CHECK (action_type IN ('BAN_USER', 'UNBAN_USER', 'DELETE_USER', 'VIEW_USER', 'DELETE_PAYMENT', 'DELETE_DOCUMENT', 'ADD_CREDITS', 'REMOVE_CREDITS', 'OTHER'));

-- Create user_credits for any existing users that don't have one (legacy fix)
INSERT INTO user_credits (user_id, chat_credits, quiz_gen_credits, updated_at)
SELECT u.id, 0, 0, NOW()
FROM users u
WHERE NOT EXISTS (SELECT 1 FROM user_credits uc WHERE uc.user_id = u.id);
