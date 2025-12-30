-- Kiểm tra payment records
SELECT 
    p.id,
    p.vnp_txn_ref,
    p.status,
    p.amount,
    p.paid_at,
    p.created_at,
    u.email as user_email,
    pl.code as plan_code,
    pl.chat_credits,
    pl.quiz_gen_credits
FROM payments p
JOIN users u ON p.user_id = u.id
JOIN plans pl ON p.plan_id = pl.id
ORDER BY p.created_at DESC
LIMIT 10;

-- Kiểm tra user credits
SELECT 
    uc.id,
    u.email,
    uc.chat_credits,
    uc.quiz_gen_credits,
    uc.expires_at,
    uc.updated_at
FROM user_credits uc
JOIN users u ON uc.user_id = u.id
WHERE u.email = 'nht.nguyenhuutai315@gmail.com';

-- Kiểm tra credit transactions
SELECT 
    ct.id,
    u.email,
    ct.transaction_type,
    ct.chat_credits_change,
    ct.quiz_gen_credits_change,
    ct.description,
    ct.created_at
FROM credit_transactions ct
JOIN users u ON ct.user_id = u.id
WHERE u.email = 'nht.nguyenhuutai315@gmail.com'
ORDER BY ct.created_at DESC
LIMIT 10;
