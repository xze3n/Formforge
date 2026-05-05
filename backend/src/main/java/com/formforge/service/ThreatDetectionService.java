package com.formforge.service;

import com.formforge.model.AuditAction;
import com.formforge.model.ObservationEntry;
import com.formforge.repository.AuditLogRepository;
import com.formforge.repository.ObservationEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * Analyses recent audit log activity per user and raises / escalates
 * {@link ObservationEntry} records for suspicious patterns.
 *
 * Detection rules:
 * <ul>
 *   <li>BRUTE_FORCE_LOGIN  – ≥5 failed logins in 10 min  → HIGH</li>
 *   <li>RAPID_DELETION     – ≥3 deletes in 5 min          → HIGH</li>
 *   <li>MASS_ENUMERATION   – ≥30 reads in 1 min           → MEDIUM</li>
 *   <li>MASS_MUTATION      – ≥10 mutations in 2 min       → MEDIUM</li>
 *   <li>REPEATED_FAILURES  – ≥5 failures of any action    → LOW</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ThreatDetectionService {

    private final AuditLogRepository auditLogRepository;
    private final ObservationEntryRepository observationEntryRepository;

    /** Runs all detection checks asynchronously so they never block the request. */
    @Async("auditTaskExecutor")
    @Transactional
    public void analyzeAsync(Long userId, String username, AuditAction action, boolean success) {
        try {
            checkBruteForceLogin(userId, username);
            checkRapidDeletion(userId, username);
            checkMassEnumeration(userId, username);
            checkMassMutation(userId, username);
            if (!success) checkRepeatedFailures(userId, username, action);
        } catch (Exception ex) {
            log.error("Threat detection error for user {}: {}", userId, ex.getMessage(), ex);
        }
    }

    // ── Detection checks ──────────────────────────────────────────────────────

    private void checkBruteForceLogin(Long userId, String username) {
        long count = auditLogRepository.countByUserIdAndActionAndSuccessAndCreatedAtAfter(
                userId, AuditAction.LOGIN_FAILURE, false,
                Instant.now().minus(10, ChronoUnit.MINUTES));
        if (count >= 5) {
            flag(userId, username, "BRUTE_FORCE_LOGIN", "HIGH",
                    AuditAction.LOGIN_FAILURE.name(),
                    count + " failed login attempts in the last 10 minutes");
        }
    }

    private void checkRapidDeletion(Long userId, String username) {
        long count = auditLogRepository.countByUserIdAndActionAndCreatedAtAfter(
                userId, AuditAction.DELETE_APPLICATION,
                Instant.now().minus(5, ChronoUnit.MINUTES));
        if (count >= 3) {
            flag(userId, username, "RAPID_DELETION", "HIGH",
                    AuditAction.DELETE_APPLICATION.name(),
                    count + " application deletions in the last 5 minutes");
        }
    }

    private void checkMassEnumeration(Long userId, String username) {
        long count = auditLogRepository.countByUserIdAndActionAndCreatedAtAfter(
                userId, AuditAction.READ_APPLICATIONS,
                Instant.now().minus(1, ChronoUnit.MINUTES));
        if (count >= 30) {
            flag(userId, username, "MASS_ENUMERATION", "MEDIUM",
                    AuditAction.READ_APPLICATIONS.name(),
                    count + " bulk-read queries in the last minute — possible data scraping");
        }
    }

    private void checkMassMutation(Long userId, String username) {
        List<AuditAction> mutations = List.of(
                AuditAction.CREATE_APPLICATION, AuditAction.UPDATE_APPLICATION,
                AuditAction.CREATE_DOCUMENT,    AuditAction.UPDATE_DOCUMENT);
        long count = auditLogRepository.countByUserIdAndActionInAndCreatedAtAfter(
                userId, mutations,
                Instant.now().minus(2, ChronoUnit.MINUTES));
        if (count >= 10) {
            flag(userId, username, "MASS_MUTATION", "MEDIUM",
                    "MUTATION",
                    count + " write operations in the last 2 minutes — possible automated abuse");
        }
    }

    private void checkRepeatedFailures(Long userId, String username, AuditAction action) {
        long count = auditLogRepository.countByUserIdAndActionAndSuccessAndCreatedAtAfter(
                userId, action, false,
                Instant.now().minus(5, ChronoUnit.MINUTES));
        if (count >= 5) {
            flag(userId, username, "REPEATED_FAILURES_" + action.name(), "LOW",
                    action.name(),
                    count + " consecutive failures for " + action.name() + " in 5 minutes");
        }
    }

    // ── Observation management ────────────────────────────────────────────────

    private void flag(Long userId, String username, String reason, String severity,
                      String triggerAction, String details) {
        Optional<ObservationEntry> existing =
                observationEntryRepository.findByUserIdAndReason(userId, reason);

        if (existing.isPresent()) {
            ObservationEntry entry = existing.get();
            entry.setOccurrenceCount(entry.getOccurrenceCount() + 1);
            entry.setDetectedAt(Instant.now());
            entry.setResolved(false); // re-surface if previously resolved
            observationEntryRepository.save(entry);
        } else {
            observationEntryRepository.save(ObservationEntry.builder()
                    .userId(userId)
                    .username(username)
                    .reason(reason)
                    .severity(severity)
                    .triggerAction(triggerAction)
                    .occurrenceCount(1)
                    .build());
        }

        log.warn("THREAT [{}] user='{}' (id={}) — {}", severity, username, userId, details);
    }
}
