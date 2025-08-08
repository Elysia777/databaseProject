-- 修复phone和email字段的空字符串冲突问题
USE taxi_system;

-- 查看当前冲突情况
SELECT 'Phone conflicts:' as info;
SELECT phone, COUNT(*) as count FROM users WHERE phone = '' GROUP BY phone;

SELECT 'Email conflicts:' as info;
SELECT email, COUNT(*) as count FROM users WHERE email = '' GROUP BY email;

-- 修复空字符串为NULL
UPDATE users SET phone = NULL WHERE phone = '';
UPDATE users SET email = NULL WHERE email = '';

-- 验证修复结果
SELECT 'After fix - Phone NULL count:' as info;
SELECT COUNT(*) as null_phone_count FROM users WHERE phone IS NULL;

SELECT 'After fix - Email NULL count:' as info;
SELECT COUNT(*) as null_email_count FROM users WHERE email IS NULL;

-- 显示所有用户信息
SELECT 'All users:' as info;
SELECT id, username, phone, email, user_type, status FROM users ORDER BY id;