-- ============================================================
-- FormForge  –  V4 Seed Users, Roles & Permissions
--
-- Two roles:
--   ADMIN – full access (all 4 permissions)
--   USER  – restricted access (read + write only)
--
-- Two seed accounts:
--   admin / admin@formforge.com  / password: admin123
--   user1 / user1@formforge.com  / password: user123
--
-- Existing applications 1-4 are assigned to admin,
-- applications 5-8 are assigned to user1.
-- ============================================================

-- ── Permissions ────────────────────────────────────────────
INSERT INTO permissions (name) VALUES
    ('READ_APPLICATIONS'),
    ('WRITE_APPLICATIONS'),
    ('DELETE_APPLICATIONS'),
    ('MANAGE_USERS');

-- ── Roles ──────────────────────────────────────────────────
INSERT INTO roles (name) VALUES
    ('ADMIN'),
    ('USER');

-- ── ADMIN gets all permissions ─────────────────────────────
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM   roles r, permissions p
WHERE  r.name = 'ADMIN';

-- ── USER gets READ + WRITE only ────────────────────────────
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM   roles r, permissions p
WHERE  r.name = 'USER'
  AND  p.name IN ('READ_APPLICATIONS', 'WRITE_APPLICATIONS');

-- ── Seed accounts ──────────────────────────────────────────
INSERT INTO users (username, email, password) VALUES
    ('admin', 'admin@formforge.com', 'admin123'),
    ('user1', 'user1@formforge.com', 'user123');

-- ── Assign roles to users ──────────────────────────────────
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM   users u, roles r
WHERE  u.username = 'admin' AND r.name = 'ADMIN';

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM   users u, roles r
WHERE  u.username = 'user1' AND r.name = 'USER';

-- ── Link existing applications to owners ───────────────────
UPDATE applications
SET    owner_id = (SELECT id FROM users WHERE username = 'admin')
WHERE  id IN (1, 2, 3, 4);

UPDATE applications
SET    owner_id = (SELECT id FROM users WHERE username = 'user1')
WHERE  id IN (5, 6, 7, 8);
