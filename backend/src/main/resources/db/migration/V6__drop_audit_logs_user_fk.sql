-- ============================================================
--  V6 – Drop FK constraint on audit_logs.user_id
--
--  audit_logs are written in a REQUIRES_NEW transaction inside
--  UserService.register(), which means the referencing user row
--  may not yet be committed when the audit insert fires. Removing
--  the FK avoids the resulting foreign-key violation while keeping
--  the user_id column for filtering/reporting.
-- ============================================================

ALTER TABLE audit_logs DROP CONSTRAINT IF EXISTS audit_logs_user_id_fkey;
