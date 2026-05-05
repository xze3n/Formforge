package com.formforge.dto;

import com.formforge.model.AuditLog;

import java.time.Instant;

public record AuditLogDto(
        Long id,
        Long userId,
        String username,
        String userRole,
        String action,
        String resourceType,
        String resourceId,
        String details,
        String ipAddress,
        boolean success,
        Instant createdAt
) {
    public static AuditLogDto from(AuditLog log) {
        return new AuditLogDto(
                log.getId(),
                log.getUserId(),
                log.getUsername(),
                log.getUserRole(),
                log.getAction().name(),
                log.getResourceType(),
                log.getResourceId(),
                log.getDetails(),
                log.getIpAddress(),
                log.isSuccess(),
                log.getCreatedAt()
        );
    }
}
