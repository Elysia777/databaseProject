-- 检查数据库中phone字段的重复情况
SELECT 
    phone, 
    COUNT(*) as count,
    GROUP_CONCAT(username) as usernames
FROM users 
WHERE phone = '' OR phone IS NULL
GROUP BY phone
HAVING COUNT(*) > 1;

-- 查看所有用户的phone字段情况
SELECT 
    id,
    username,
    phone,
    email,
    user_type,
    status
FROM users 
ORDER BY id;

-- 更新所有空字符串的phone为NULL（避免唯一约束冲突）
UPDATE users 
SET phone = NULL 
WHERE phone = '';

-- 更新所有空字符串的email为NULL（避免唯一约束冲突）
UPDATE users 
SET email = NULL 
WHERE email = '';