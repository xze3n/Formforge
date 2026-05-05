-- ============================================================
-- FormForge  –  V1 Initial Schema
-- Normalisation: both tables are in 3NF
--   applications: all non-key columns depend only on id
--   documents:    all non-key columns depend only on id;
--                 application_id is a referential attribute
-- ============================================================

CREATE TABLE applications (
    id           BIGSERIAL    PRIMARY KEY,
    type         VARCHAR(20)  NOT NULL,
    academic_year VARCHAR(20) NOT NULL,
    semester     VARCHAR(5)   NOT NULL,
    created_at   VARCHAR(20)  NOT NULL,
    status       VARCHAR(20)  NOT NULL
);

CREATE TABLE documents (
    id             BIGSERIAL    PRIMARY KEY,
    application_id BIGINT       NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
    name           VARCHAR(255) NOT NULL,
    type           VARCHAR(100) NOT NULL,
    description    TEXT,
    date_added     VARCHAR(20),
    verified       BOOLEAN      NOT NULL DEFAULT FALSE,
    notes          TEXT
);

-- Indexes for common filter / join columns
CREATE INDEX idx_applications_status   ON applications(status);
CREATE INDEX idx_applications_type     ON applications(type);
CREATE INDEX idx_applications_semester ON applications(semester);
CREATE INDEX idx_documents_application ON documents(application_id);

-- ---------------------------------------------------------------
-- Trigger: enforce DRAFT default at DB level (before insert)
-- ---------------------------------------------------------------
CREATE OR REPLACE FUNCTION trg_applications_defaults()
    RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.status IS NULL OR NEW.status = '' THEN
        NEW.status := 'DRAFT';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER applications_before_insert
    BEFORE INSERT ON applications
    FOR EACH ROW EXECUTE FUNCTION trg_applications_defaults();

-- ---------------------------------------------------------------
-- Stored procedure: aggregate application statistics
--   Returns three JSON arrays (one per dimension) via OUT params.
--   Usage: CALL get_application_statistics(NULL, NULL, NULL);
-- ---------------------------------------------------------------
CREATE OR REPLACE PROCEDURE get_application_statistics(
    OUT by_status  TEXT,
    OUT by_type    TEXT,
    OUT by_semester TEXT
) LANGUAGE plpgsql AS $$
BEGIN
    SELECT json_agg(row_to_json(s)) INTO by_status
    FROM (SELECT status AS key, COUNT(*) AS count
          FROM applications GROUP BY status) s;

    SELECT json_agg(row_to_json(t)) INTO by_type
    FROM (SELECT type AS key, COUNT(*) AS count
          FROM applications GROUP BY type) t;

    SELECT json_agg(row_to_json(sm)) INTO by_semester
    FROM (SELECT semester AS key, COUNT(*) AS count
          FROM applications GROUP BY semester) sm;
END;
$$;
