package com.formforge.controller;

import com.formforge.config.AuditContextHolder;
import com.formforge.dto.AuditLogDto;
import com.formforge.dto.ObservationEntryDto;
import com.formforge.dto.PageResponse;
import com.formforge.model.AuditAction;
import com.formforge.model.ObservationEntry;
import com.formforge.repository.AuditLogRepository;
import com.formforge.repository.ObservationEntryRepository;
import com.formforge.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Map;

/**
 * Admin-only REST endpoints for audit log inspection and observation management.
 * Role enforcement is done in-method (frontend ADMIN guard + header-based role check).
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AuditController {

    private final AuditLogRepository    auditLogRepository;
    private final ObservationEntryRepository observationRepo;
    private final AuditLogService       auditLogService;

    // ── Audit Logs ────────────────────────────────────────────────────────────

    @GetMapping("/audit-logs")
    public PageResponse<AuditLogDto> getAuditLogs(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false)    Long   userId,
            @RequestParam(required = false)    String action) {

        requireAdmin();
        auditLogService.logAuth(
                ctxUserId(), ctxUsername(), ctxRole(),
                AuditAction.ADMIN_VIEW_LOGS, null, ctxIp(), true);

        PageRequest pr = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AuditLogDto> result;

        if (userId != null) {
            result = auditLogRepository.findByUserId(userId, pr).map(AuditLogDto::from);
        } else if (action != null && !action.isBlank()) {
            AuditAction act = AuditAction.valueOf(action.toUpperCase());
            result = auditLogRepository.findByAction(act, pr).map(AuditLogDto::from);
        } else {
            result = auditLogRepository.findAllByOrderByCreatedAtDesc(pr).map(AuditLogDto::from);
        }

        return new PageResponse<>(result.getContent(), page, size,
                result.getTotalElements(), result.getTotalPages());
    }

    // ── Observation List ──────────────────────────────────────────────────────

    @GetMapping("/observations")
    public PageResponse<ObservationEntryDto> getObservations(
            @RequestParam(defaultValue = "0")     int     page,
            @RequestParam(defaultValue = "50")    int     size,
            @RequestParam(defaultValue = "false") boolean unresolvedOnly) {

        requireAdmin();
        auditLogService.logAuth(
                ctxUserId(), ctxUsername(), ctxRole(),
                AuditAction.ADMIN_VIEW_OBSERVATIONS, null, ctxIp(), true);

        PageRequest pr = PageRequest.of(page, size);
        Page<ObservationEntryDto> result = unresolvedOnly
                ? observationRepo.findByResolvedFalseOrderByDetectedAtDesc(pr).map(ObservationEntryDto::from)
                : observationRepo.findAllByOrderByDetectedAtDesc(pr).map(ObservationEntryDto::from);

        return new PageResponse<>(result.getContent(), page, size,
                result.getTotalElements(), result.getTotalPages());
    }

    @GetMapping("/observations/count")
    public Map<String, Long> getUnresolvedCount() {
        requireAdmin();
        return Map.of("unresolved", observationRepo.countByResolvedFalse());
    }

    @PostMapping("/observations/{id}/resolve")
    public ObservationEntryDto resolveObservation(@PathVariable Long id) {
        requireAdmin();

        ObservationEntry entry = observationRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Observation not found: " + id));

        entry.setResolved(true);
        entry.setResolvedAt(Instant.now());
        entry.setResolvedBy(ctxUsername());
        observationRepo.save(entry);

        auditLogService.logAuth(
                ctxUserId(), ctxUsername(), ctxRole(),
                AuditAction.ADMIN_RESOLVE_OBSERVATION,
                "Resolved observation id=" + id, ctxIp(), true);

        return ObservationEntryDto.from(entry);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void requireAdmin() {
        AuditContextHolder.AuditContext ctx = AuditContextHolder.get();
        if (ctx == null || !"ADMIN".equalsIgnoreCase(ctx.getUserRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Admin access required");
        }
    }

    private Long   ctxUserId()   { AuditContextHolder.AuditContext c = AuditContextHolder.get(); return c != null ? c.getUserId()  : null; }
    private String ctxUsername() { AuditContextHolder.AuditContext c = AuditContextHolder.get(); return c != null ? c.getUsername() : null; }
    private String ctxRole()     { AuditContextHolder.AuditContext c = AuditContextHolder.get(); return c != null ? c.getUserRole() : null; }
    private String ctxIp()       { AuditContextHolder.AuditContext c = AuditContextHolder.get(); return c != null ? c.getIpAddress(): null; }
}
