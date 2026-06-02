-- ============================================================
--  V10 – Restore seed account roles & fix primaryRole ordering
--
--  The DataSeeder (ApplicationRunner) assigned random extra roles
--  (AUDITOR, VIEWER, EDITOR …) to all existing users including the
--  two seed accounts.  This migration strips those extra roles and
--  restores the intended assignment:
--    admin@formforge.com  → ADMIN only
--    user1@formforge.com  → USER  only
-- ============================================================

-- Remove ALL role assignments for the two seed accounts
DELETE FROM user_roles
WHERE user_id IN (
    SELECT id FROM users WHERE email IN ('admin@formforge.com', 'user1@formforge.com')
);

-- Re-assign ADMIN role to admin account
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.email = 'admin@formforge.com'
  AND r.name  = 'ADMIN';

-- Re-assign USER role to user1 account
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.email = 'user1@formforge.com'
  AND r.name  = 'USER';
