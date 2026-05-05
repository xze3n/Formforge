package com.formforge.service;

import com.formforge.model.AuditAction;
import com.formforge.model.AuditLog;
import com.formforge.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Central service for persisting audit log entries.
 * Each call runs in its own transaction so log entries are always
 * committed even if the outer business transaction rolls back.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final ThreatDetectionService threatDetectionService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(Long userId, String username, String userRole,
                    AuditAction action,
                    String resourceType, String resourceId,
                    String details, String ipAddress,
                    boolean success) {

        AuditLog entry = AuditLog.builder()
                .userId(userId)
                .username(username)
                .userRole(userRole)
                .action(action)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .details(details)
                .ipAddress(ipAddress)
                .success(success)
                .build();

        auditLogRepository.save(entry);

        // Async threat analysis — does not block the request
        if (userId != null) {
            threatDetectionService.analyzeAsync(userId, username, action, success);
        }
    }

    /** Convenience overload for auth events (no resource context). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAuth(Long userId, String username, String userRole,
                        AuditAction action, String details, String ipAddress,
                        boolean success) {
        log(userId, username, userRole, action, null, null, details, ipAddress, success);
    }
}
