package com.formforge.dto;

/**
 * Projection returned by the permission-distribution statistics endpoint.
 *
 * <p>Each record describes one permission and how many distinct users currently
 * hold it (transitively, via the many-to-many user_roles → role_permissions chain).
 *
 * @param permissionId   Primary key of the permission row.
 * @param permissionName Human-readable permission name (e.g. {@code READ_APPLICATIONS}).
 * @param userCount      Number of distinct users who have this permission through any role.
 * @param roleCount      Number of roles that grant this permission.
 * @param roleNames      Comma-separated list of those role names (for display purposes).
 */
public record PermissionDistributionDto(
        Long   permissionId,
        String permissionName,
        long   userCount,
        long   roleCount,
        String roleNames
) {}
