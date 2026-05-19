package com.formforge.repository;

import com.formforge.dto.PermissionDistributionDto;
import com.formforge.model.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Persistence operations for the {@code permissions} table.
 *
 * <p>Extends the standard CRUD provided by {@link JpaRepository} with two
 * additional queries used by the statistics endpoint:
 *
 * <ul>
 *   <li>{@link #findPermissionDistribution()} – a <em>live</em> 3-table join
 *       computed fresh on every call (the <em>naive</em> implementation used to
 *       demonstrate the un-optimised baseline).</li>
 *   <li>{@link #findPermissionDistributionFromMaterializedView()} – reads the
 *       pre-aggregated {@code mv_permission_distribution} materialized view
 *       created by migration V8; returns in milliseconds regardless of dataset
 *       size.</li>
 * </ul>
 */
public interface PermissionStatsRepository extends JpaRepository<Permission, Long> {

    /**
     * Naive 2-hop M:N join: permissions → role_permissions → user_roles.
     *
     * <p>Scans and GROUP BY / COUNT(DISTINCT …) across the full
     * {@code user_roles} table on every invocation. With 10 000 users and
     * 20 000–30 000 join rows the query takes tens of milliseconds; under
     * sustained concurrent load (DDoS) the PostgreSQL worker pool saturates and
     * latency climbs sharply.
     */
    @Query(value = """
            SELECT
                p.id                                     AS permission_id,
                p.name                                   AS permission_name,
                COALESCE(COUNT(DISTINCT ur.user_id), 0)  AS user_count,
                COALESCE(COUNT(DISTINCT rp.role_id),  0) AS role_count,
                STRING_AGG(DISTINCT r.name, ', ' ORDER BY r.name)
                                                         AS role_names
            FROM permissions p
            LEFT JOIN role_permissions rp ON rp.permission_id = p.id
            LEFT JOIN roles             r  ON r.id             = rp.role_id
            LEFT JOIN user_roles        ur ON ur.role_id        = rp.role_id
            GROUP BY p.id, p.name
            ORDER BY user_count DESC
            """,
            nativeQuery = true)
    List<PermissionDistributionProjection> findPermissionDistribution();

    /**
     * Reads directly from the {@code mv_permission_distribution} materialized
     * view created in migration V8. The data was pre-aggregated at the last
     * refresh, so this is a simple indexed scan with no computation overhead.
     */
    @Query(value = """
            SELECT
                permission_id,
                permission_name,
                user_count,
                role_count,
                role_names
            FROM mv_permission_distribution
            ORDER BY user_count DESC
            """,
            nativeQuery = true)
    List<PermissionDistributionProjection> findPermissionDistributionFromMaterializedView();

    /**
     * Spring Data JPA projection interface.
     * Column aliases in the native SQL must exactly match these method names.
     */
    interface PermissionDistributionProjection {
        Long   getPermission_id();
        String getPermission_name();
        Long   getUser_count();
        Long   getRole_count();
        String getRole_names();
    }
}
