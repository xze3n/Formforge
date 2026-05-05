-- ============================================================
-- FormForge  –  V2 Seed Data
-- 8 sample applications + 5 documents (mirrors former InMemory seed)
-- ============================================================

INSERT INTO applications (type, academic_year, semester, created_at, status) VALUES
    ('MERIT',        '2024/2025', 'I',  '1/15/2025',  'APPROVED'),
    ('SOCIAL',       '2024/2025', 'II', '2/3/2025',   'PENDING_ACTION'),
    ('PERFORMANCE',  '2023/2024', 'I',  '9/10/2024',  'DRAFT'),
    ('MERIT',        '2023/2024', 'II', '1/20/2024',  'APPROVED'),
    ('SOCIAL',       '2025/2026', 'I',  '9/5/2025',   'DRAFT'),
    ('PERFORMANCE',  '2024/2025', 'I',  '10/12/2024', 'PENDING_ACTION'),
    ('MERIT',        '2022/2023', 'II', '2/28/2023',  'APPROVED'),
    ('SOCIAL',       '2023/2024', 'I',  '11/1/2023',  'DRAFT');

INSERT INTO documents (application_id, name, type, description, date_added, verified, notes) VALUES
    (1, 'Enrollment Certificate', 'STUDENT_ENROLLMENT_CERTIFICATE', 'Official enrollment proof',     '1/15/2025', true,  'Verified by office'),
    (1, 'ID Copy',                'ID_COPY',                        'National ID scan',              '1/15/2025', true,  NULL),
    (2, 'Income Certificate',     'INCOME_CERTIFICATE',             'Family monthly income details', '2/3/2025',  false, 'Needs stamp'),
    (3, 'Social Assessment',      'SOCIAL_ASSESSMENT_REPORT',       'Social worker report',          '9/10/2024', false, NULL),
    (4, 'Tax Certificate',        'TAX_CERTIFICATE',                'Annual tax clearance',          '1/20/2024', true,  'Reviewed');
