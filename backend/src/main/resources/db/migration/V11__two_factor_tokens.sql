-- ============================================================
--  V11 – Two-factor authentication (email-link style)
--
--  After a successful password check, a short-lived single-use
--  token is generated and "sent" to the user (logged to stdout).
--  The user must POST the token to /api/auth/verify-2fa to
--  receive their JWT.
-- ============================================================

CREATE TABLE two_factor_tokens (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token       VARCHAR(64) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ NOT NULL,
    used        BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_2fa_token  ON two_factor_tokens (token);
CREATE INDEX idx_2fa_user   ON two_factor_tokens (user_id);
