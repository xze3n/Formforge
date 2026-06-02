-- ============================================================
--  V12 – Chat Messages table (replaces MongoDB persistence)
-- ============================================================

CREATE TABLE chat_messages (
    id          BIGSERIAL    PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    username    VARCHAR(100) NOT NULL,
    text        TEXT         NOT NULL,
    timestamp   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_chat_timestamp ON chat_messages(timestamp DESC);
