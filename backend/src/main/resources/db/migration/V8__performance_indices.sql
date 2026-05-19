-- ============================================================
--  V8 – Performance Indices & Materialized View
--
--  Purpose:
--    1. Add the missing reverse-direction index on role_permissions
--       so the permission-distribution query (which joins FROM permissions
--       TO role_permissions) can use an index scan instead of a seq scan.
--
--    2. Create a materialized view that pre-computes the expensive
--       2-hop M:N aggregate:
--          permissions → role_permissions → user_roles
--       The view stores one row per permission with:
--         • user_count  – distinct users who have this permission via any role
--         • role_count  – number of roles that grant this permission
--         • role_names  – comma-separated list of those role names
--
--    3. Add a UNIQUE index on the MV so REFRESH MATERIALIZED VIEW
--       CONCURRENTLY is supported (no read-lock on the view during refresh).
-- ============================================================

-- ── 1. Reverse-direction index on role_permissions ────────────────────────
--  The existing PK (role_id, permission_id) covers JOINs in the role→permission
--  direction. The query below joins permission→role, so a (permission_id) index
--  is needed for an efficient index scan on 50 k+ rows.
CREATE INDEX IF NOT EXISTS idx_role_permissions_permission
    ON role_permissions (permission_id);

-- ── 2. Materialized view: permission distribution ─────────────────────────
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_permission_distribution AS
SELECT
    p.id                                    AS permission_id,
    p.name                                  AS permission_name,
    COALESCE(COUNT(DISTINCT ur.user_id), 0) AS user_count,
    COALESCE(COUNT(DISTINCT rp.role_id),  0) AS role_count,
    STRING_AGG(DISTINCT r.name, ', ' ORDER BY r.name)
                                            AS role_names
FROM permissions p
LEFT JOIN role_permissions rp ON rp.permission_id = p.id
LEFT JOIN roles             r  ON r.id             = rp.role_id
LEFT JOIN user_roles        ur ON ur.role_id        = rp.role_id
GROUP BY p.id, p.name
ORDER BY user_count DESC;

-- ── 3. Unique index on the materialized view ──────────────────────────────
--  Required for REFRESH MATERIALIZED VIEW CONCURRENTLY (non-blocking refresh).
CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_perm_dist_permission_id
    ON mv_permission_distribution (permission_id);

-- ── 4. Refresh helper function ────────────────────────────────────────────
--  Called by the Spring @Scheduled task every 10 minutes so the view stays
--  current as new users are added, without blocking concurrent reads.
CREATE OR REPLACE FUNCTION refresh_permission_distribution()
    RETURNS void LANGUAGE plpgsql AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_permission_distribution;
END;
$$;
