package com.formforge.seeder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Seed the database with a large, realistic dataset using DataFaker.
 *
 * <p>Runs once at startup. Checks the current user count first; if the
 * database already has ≥ {@value #TARGET_USER_COUNT} users the seeder
 * skips entirely so restarts are idempotent.
 *
 * <h3>What is seeded</h3>
 * <ul>
 *   <li>18 additional named roles  (VIEWER, EDITOR, REVIEWER, …)</li>
 *   <li>46 additional named permissions  (SUBMIT_APPLICATION, …)</li>
 *   <li>~15 random permission-role assignments per new role</li>
 *   <li>10 000 users with realistic names / e-mails (DataFaker)</li>
 *   <li>1–3 random roles per user  →  ~20 000–30 000 user_role rows</li>
 *   <li>5 random applications per user  →  ~50 000 application rows</li>
 * </ul>
 *
 * <p>All inserts use JDBC batch operations for performance.
 * BCrypt encoding is called ONCE; the resulting hash is reused for
 * all fake users to avoid a ~16-minute hashing delay.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private static final int TARGET_USER_COUNT = 10_000;
    private static final int BATCH_SIZE        = 500;

    private final JdbcTemplate    jdbc;
    private final PasswordEncoder passwordEncoder;

    // ─── Supplementary roles / permissions ───────────────────────────────

    private static final List<String> EXTRA_ROLES = List.of(
            "VIEWER", "EDITOR", "REVIEWER", "APPROVER",
            "SUPERVISOR", "AUDITOR", "MANAGER", "COORDINATOR",
            "ANALYST", "CONSULTANT", "MODERATOR", "OPERATOR",
            "DEVELOPER", "TESTER", "SUPPORT", "READONLY",
            "POWER_USER", "GUEST"
    );

    private static final List<String> EXTRA_PERMISSIONS = List.of(
            "SUBMIT_APPLICATION",      "REVIEW_APPLICATION",
            "APPROVE_APPLICATION",     "REJECT_APPLICATION",
            "ARCHIVE_APPLICATION",     "EXPORT_APPLICATIONS",
            "BULK_UPDATE_APPLICATIONS","REASSIGN_APPLICATION",
            "VIEW_ALL_APPLICATIONS",   "VIEW_OWN_APPLICATIONS",
            "VIEW_AUDIT_LOGS",         "EXPORT_AUDIT_LOGS",
            "MANAGE_ROLES",            "MANAGE_PERMISSIONS",
            "INVITE_USER",             "DEACTIVATE_USER",
            "RESET_USER_PASSWORD",     "VIEW_USER_PROFILES",
            "UPLOAD_DOCUMENT",         "VERIFY_DOCUMENT",
            "DELETE_DOCUMENT",         "DOWNLOAD_DOCUMENT",
            "VIEW_STATISTICS",         "EXPORT_STATISTICS",
            "VIEW_OBSERVATION_LIST",   "RESOLVE_OBSERVATION",
            "CONFIGURE_THREAT_RULES",  "VIEW_CHAT_HISTORY",
            "DELETE_CHAT_MESSAGES",    "CONFIGURE_NOTIFICATIONS",
            "VIEW_SYSTEM_HEALTH",      "MANAGE_API_KEYS",
            "READ_REFRESH_TOKENS",     "REVOKE_REFRESH_TOKENS",
            "SCHEDULE_MAINTENANCE",    "ACCESS_GRAPHQL",
            "ACCESS_GRAPHIQL",         "ACCESS_WEBSOCKET",
            "VIEW_PERMISSION_STATS",   "REFRESH_MATERIALIZED_VIEWS",
            "MANAGE_ACADEMIC_YEARS",   "CONFIGURE_SEMESTER_RULES",
            "GENERATE_FAKE_DATA",      "PURGE_SEEDED_DATA",
            "ACCESS_JMETER_ENDPOINT",  "VIEW_CACHE_METRICS"
    );

    private static final String[] APP_TYPES     = {"MERIT", "SOCIAL", "PERFORMANCE"};
    private static final String[] SEMESTERS     = {"I", "II"};
    private static final String[] STATUSES      = {"DRAFT", "PENDING_ACTION", "APPROVED"};
    private static final String[] ACADEMIC_YEARS = {
            "2021/2022", "2022/2023", "2023/2024", "2024/2025", "2025/2026"
    };

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("M/d/yyyy");

    // ─── Entry point ─────────────────────────────────────────────────────

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        long existingUsers = jdbc.queryForObject("SELECT COUNT(*) FROM users", Long.class);
        if (existingUsers >= TARGET_USER_COUNT) {
            log.info("[DataSeeder] {} users already present – skipping seed.", existingUsers);
            return;
        }

        log.info("[DataSeeder] Starting large-dataset seed (existing users: {}) …", existingUsers);
        long start = System.currentTimeMillis();

        List<Long> roleIds       = seedRoles();
        List<Long> permissionIds = seedPermissions();
        seedRolePermissions(roleIds, permissionIds);

        String sharedHash = passwordEncoder.encode("Password1!");
        List<Long> userIds = seedUsers(sharedHash);
        seedUserRoles(userIds, roleIds);
        seedApplications(userIds);

        long elapsed = System.currentTimeMillis() - start;
        log.info("[DataSeeder] Seed complete in {} ms. Users: {}, Roles: {}, Permissions: {}",
                elapsed, userIds.size(), roleIds.size(), permissionIds.size());
    }

    // ─── Seed helpers ─────────────────────────────────────────────────────

    /** Insert EXTRA_ROLES that do not already exist; return all role IDs. */
    private List<Long> seedRoles() {
        for (String name : EXTRA_ROLES) {
            jdbc.update("INSERT INTO roles (name) VALUES (?) ON CONFLICT (name) DO NOTHING", name);
        }
        return jdbc.queryForList("SELECT id FROM roles ORDER BY id", Long.class);
    }

    /** Insert EXTRA_PERMISSIONS that do not already exist; return all permission IDs. */
    private List<Long> seedPermissions() {
        for (String name : EXTRA_PERMISSIONS) {
            jdbc.update("INSERT INTO permissions (name) VALUES (?) ON CONFLICT (name) DO NOTHING", name);
        }
        return jdbc.queryForList("SELECT id FROM permissions ORDER BY id", Long.class);
    }

    /**
     * Assign ~15 random permissions to each role (skip roles that already have
     * many assignments to avoid violating the PK constraint).
     */
    private void seedRolePermissions(List<Long> roleIds, List<Long> permissionIds) {
        Random rng = new Random(42);
        List<Object[]> batch = new ArrayList<>();

        for (Long roleId : roleIds) {
            List<Long> shuffled = new ArrayList<>(permissionIds);
            Collections.shuffle(shuffled, rng);
            int count = 10 + rng.nextInt(11); // 10–20 perms per role
            for (Long permId : shuffled.subList(0, Math.min(count, shuffled.size()))) {
                batch.add(new Object[]{roleId, permId});
            }
        }

        jdbc.batchUpdate(
                "INSERT INTO role_permissions (role_id, permission_id) VALUES (?, ?) ON CONFLICT DO NOTHING",
                batch
        );
        log.info("[DataSeeder] role_permissions rows inserted: ~{}", batch.size());
    }

    /**
     * Generate 10 000 users with DataFaker; returns their IDs.
     * Uses JDBC batch inserts in chunks of {@value #BATCH_SIZE}.
     */
    private List<Long> seedUsers(String sharedHash) {
        Faker faker  = new Faker(Locale.ENGLISH, new Random(123));
        int needed   = TARGET_USER_COUNT - (int) (long) jdbc.queryForObject(
                "SELECT COUNT(*) FROM users", Long.class);

        log.info("[DataSeeder] Inserting {} users in batches of {} …", needed, BATCH_SIZE);

        AtomicInteger collisionCount = new AtomicInteger(0);
        List<Object[]> batch = new ArrayList<>(BATCH_SIZE);

        for (int i = 0; i < needed; i++) {
            // Append a counter to guarantee uniqueness even if Faker produces duplicates
            String username = sanitize(faker.internet().username()) + "_" + i;
            String email    = "seed_" + i + "_" + sanitize(faker.internet().emailAddress());

            batch.add(new Object[]{username, email, sharedHash});

            if (batch.size() == BATCH_SIZE) {
                flushUserBatch(batch, collisionCount);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            flushUserBatch(batch, collisionCount);
        }

        log.info("[DataSeeder] User insert complete (collisions skipped: {}).", collisionCount.get());
        return jdbc.queryForList("SELECT id FROM users ORDER BY id", Long.class);
    }

    private void flushUserBatch(List<Object[]> batch, AtomicInteger collisions) {
        int[] counts = jdbc.batchUpdate(
                "INSERT INTO users (username, email, password) VALUES (?, ?, ?) ON CONFLICT DO NOTHING",
                batch
        );
        for (int c : counts) {
            if (c == 0) collisions.incrementAndGet();
        }
    }

    /**
     * Assign 1–3 random roles to each user via the user_roles join table.
     */
    private void seedUserRoles(List<Long> userIds, List<Long> roleIds) {
        Random rng = new Random(777);
        List<Object[]> batch = new ArrayList<>(BATCH_SIZE);

        for (Long userId : userIds) {
            int count = 1 + rng.nextInt(3); // 1, 2, or 3 roles
            List<Long> shuffled = new ArrayList<>(roleIds);
            Collections.shuffle(shuffled, rng);
            for (Long roleId : shuffled.subList(0, Math.min(count, shuffled.size()))) {
                batch.add(new Object[]{userId, roleId});
            }
            if (batch.size() >= BATCH_SIZE) {
                jdbc.batchUpdate(
                        "INSERT INTO user_roles (user_id, role_id) VALUES (?, ?) ON CONFLICT DO NOTHING",
                        batch
                );
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            jdbc.batchUpdate(
                    "INSERT INTO user_roles (user_id, role_id) VALUES (?, ?) ON CONFLICT DO NOTHING",
                    batch
            );
        }
        log.info("[DataSeeder] user_roles seeded for {} users.", userIds.size());
    }

    /**
     * Create ~5 applications per user with random types / semesters / statuses.
     */
    private void seedApplications(List<Long> userIds) {
        Random rng = new Random(555);
        Faker faker = new Faker(Locale.ENGLISH, new Random(444));
        List<Object[]> batch = new ArrayList<>(BATCH_SIZE);

        // Only add applications for users who don't already have 5+
        long existing = jdbc.queryForObject("SELECT COUNT(*) FROM applications", Long.class);
        if (existing >= (long) userIds.size() * 4) {
            log.info("[DataSeeder] Applications already seeded ({}) – skipping.", existing);
            return;
        }

        for (Long userId : userIds) {
            int count = 3 + rng.nextInt(5); // 3–7 apps per user
            for (int j = 0; j < count; j++) {
                String type         = APP_TYPES[rng.nextInt(APP_TYPES.length)];
                String semester     = SEMESTERS[rng.nextInt(SEMESTERS.length)];
                String status       = STATUSES[rng.nextInt(STATUSES.length)];
                String academicYear = ACADEMIC_YEARS[rng.nextInt(ACADEMIC_YEARS.length)];
                String createdAt    = randomDate(rng, faker);

                batch.add(new Object[]{type, academicYear, semester, createdAt, status, userId});
            }
            if (batch.size() >= BATCH_SIZE) {
                jdbc.batchUpdate(
                        "INSERT INTO applications (type, academic_year, semester, created_at, status, owner_id) " +
                        "VALUES (?, ?, ?, ?, ?, ?)",
                        batch
                );
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            jdbc.batchUpdate(
                    "INSERT INTO applications (type, academic_year, semester, created_at, status, owner_id) " +
                    "VALUES (?, ?, ?, ?, ?, ?)",
                    batch
            );
        }
        long total = jdbc.queryForObject("SELECT COUNT(*) FROM applications", Long.class);
        log.info("[DataSeeder] applications table now contains {} rows.", total);
    }

    // ─── Utility ─────────────────────────────────────────────────────────

    /** Remove characters that could cause issues in usernames / e-mails. */
    private static String sanitize(String s) {
        return s.replaceAll("[^a-zA-Z0-9._@+\\-]", "_").toLowerCase(Locale.ROOT);
    }

    /** Return a random M/d/yyyy date string within 2021–2026. */
    private static String randomDate(Random rng, Faker faker) {
        LocalDate base = LocalDate.of(2021, 1, 1);
        int daysRange  = (int) (LocalDate.of(2026, 5, 19).toEpochDay() - base.toEpochDay());
        return base.plusDays(rng.nextInt(daysRange)).format(DATE_FMT);
    }
}
