-- ============================================================
--  V5 – Audit Logging & Threat-Detection Observation List
-- ============================================================

CREATE TABLE audit_logs (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT REFERENCES users(id) ON DELETE SET NULL,
    username      VARCHAR(100),
    user_role     VARCHAR(50),
    action        VARCHAR(100) NOT NULL,
    resource_type VARCHAR(100),
    resource_id   VARCHAR(255),
    details       TEXT,
    ip_address    VARCHAR(50),
    success       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_user_id  ON audit_logs(user_id);
CREATE INDEX idx_audit_created  ON audit_logs(created_at DESC);
CREATE INDEX idx_audit_action   ON audit_logs(action);
CREATE INDEX idx_audit_success  ON audit_logs(success);

-- ============================================================

CREATE TABLE observation_list (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    username         VARCHAR(100),
    reason           VARCHAR(200) NOT NULL,
    severity         VARCHAR(20)  NOT NULL DEFAULT 'MEDIUM',  -- LOW | MEDIUM | HIGH | CRITICAL
    detected_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    resolved         BOOLEAN      NOT NULL DEFAULT FALSE,
    resolved_at      TIMESTAMPTZ,
    resolved_by      VARCHAR(100),
    trigger_action   VARCHAR(100),
    occurrence_count INT          NOT NULL DEFAULT 1,
    CONSTRAINT uq_obs_user_reason UNIQUE (user_id, reason)
);

CREATE INDEX idx_obs_user_id  ON observation_list(user_id);
CREATE INDEX idx_obs_resolved ON observation_list(resolved);
