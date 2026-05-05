-- ============================================================
-- FormForge  –  V3 Users, Roles & Permissions
--
-- Design:
--   permissions: atomic capabilities (READ_APPLICATIONS, etc.)
--   roles:       named sets of permissions (ADMIN, USER)
--   role_permissions: M:N join between roles and permissions
--   users:       registered accounts
--   user_roles:  M:N join between users and roles
--   applications.owner_id: FK linking each application to its creator
--
-- NOTE: passwords are stored as plain text – this is intentional
--       for this assignment (persistence focus only, no encryption).
--       DO NOT use this schema in production.
-- ============================================================

-- ── Permissions ────────────────────────────────────────────
CREATE TABLE permissions (
    id   BIGSERIAL    PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

-- ── Roles ──────────────────────────────────────────────────
CREATE TABLE roles (
    id   BIGSERIAL   PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

-- ── Role ↔ Permission (M:N) ────────────────────────────────
CREATE TABLE role_permissions (
    role_id       BIGINT NOT NULL REFERENCES roles(id)       ON DELETE CASCADE,
    permission_id BIGINT NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

-- ── Users ──────────────────────────────────────────────────
CREATE TABLE users (
    id       BIGSERIAL    PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    email    VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL  -- plain text (assignment only)
);

-- ── User ↔ Role (M:N) ──────────────────────────────────────
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- ── Application ownership ──────────────────────────────────
ALTER TABLE applications
    ADD COLUMN owner_id BIGINT REFERENCES users(id) ON DELETE SET NULL;

CREATE INDEX idx_applications_owner ON applications(owner_id);
CREATE INDEX idx_user_roles_user    ON user_roles(user_id);
CREATE INDEX idx_user_roles_role    ON user_roles(role_id);
