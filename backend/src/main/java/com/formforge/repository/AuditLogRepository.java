package com.formforge.repository;

import com.formforge.model.AuditAction;
import com.formforge.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<AuditLog> findByUserId(Long userId, Pageable pageable);

    Page<AuditLog> findByAction(AuditAction action, Pageable pageable);

    long countByUserIdAndActionAndCreatedAtAfter(
            Long userId, AuditAction action, Instant after);

    long countByUserIdAndActionAndSuccessAndCreatedAtAfter(
            Long userId, AuditAction action, boolean success, Instant after);

    long countByUserIdAndActionInAndCreatedAtAfter(
            Long userId, List<AuditAction> actions, Instant after);
}
