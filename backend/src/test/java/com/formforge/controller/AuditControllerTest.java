package com.formforge.controller;

import com.formforge.config.AuditContextHolder;
import com.formforge.dto.AuditLogDto;
import com.formforge.dto.ObservationEntryDto;
import com.formforge.dto.PageResponse;
import com.formforge.model.AuditAction;
import com.formforge.model.AuditLog;
import com.formforge.model.ObservationEntry;
import com.formforge.repository.AuditLogRepository;
import com.formforge.repository.ObservationEntryRepository;
import com.formforge.service.AuditLogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditControllerTest {

    @Mock private AuditLogRepository auditLogRepository;
    @Mock private ObservationEntryRepository observationRepo;
    @Mock private AuditLogService auditLogService;

    private AuditController controller;

    @BeforeEach
    void setUp() {
        controller = new AuditController(auditLogRepository, observationRepo, auditLogService);
        setAdminContext();
    }

    @AfterEach
    void cleanup() {
        AuditContextHolder.clear();
    }

    // Admin access is enforced by @PreAuthorize("hasRole('ADMIN')") at the class level,
    // which requires Spring Security AOP and cannot be verified in a plain unit test.
    // Access control is covered by integration / security layer tests.

    // ── getAuditLogs ─────────────────────────────────────────────────────────

    @Test
    void getAuditLogs_noFilter_returnsAllLogs() {
        AuditLog log = buildLog(1L, AuditAction.LOGIN_SUCCESS);
        Page<AuditLog> page = new PageImpl<>(List.of(log));
        when(auditLogRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class)))
                .thenReturn(page);

        PageResponse<AuditLogDto> result = controller.getAuditLogs(0, 10, null, null);

        assertEquals(1, result.getContent().size());
        assertEquals("LOGIN_SUCCESS", result.getContent().get(0).action());
        verify(auditLogService).logAuth(anyLong(), anyString(), anyString(),
                eq(AuditAction.ADMIN_VIEW_LOGS), isNull(), isNull(), eq(true));
    }

    @Test
    void getAuditLogs_filterByUserId() {
        AuditLog log = buildLog(2L, AuditAction.CREATE_APPLICATION);
        log.setUserId(5L);
        Page<AuditLog> page = new PageImpl<>(List.of(log));
        when(auditLogRepository.findByUserId(eq(5L), any(Pageable.class))).thenReturn(page);

        PageResponse<AuditLogDto> result = controller.getAuditLogs(0, 10, 5L, null);

        assertEquals(1, result.getContent().size());
        verify(auditLogRepository).findByUserId(eq(5L), any());
    }

    @Test
    void getAuditLogs_filterByAction() {
        AuditLog log = buildLog(3L, AuditAction.DELETE_APPLICATION);
        Page<AuditLog> page = new PageImpl<>(List.of(log));
        when(auditLogRepository.findByAction(eq(AuditAction.DELETE_APPLICATION), any(Pageable.class)))
                .thenReturn(page);

        PageResponse<AuditLogDto> result = controller.getAuditLogs(0, 10, null, "DELETE_APPLICATION");

        assertEquals(1, result.getContent().size());
        verify(auditLogRepository).findByAction(eq(AuditAction.DELETE_APPLICATION), any());
    }

    @Test
    void getAuditLogs_filterByBlankAction_returnsAll() {
        Page<AuditLog> page = new PageImpl<>(List.of());
        when(auditLogRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class))).thenReturn(page);

        controller.getAuditLogs(0, 10, null, "  ");

        verify(auditLogRepository).findAllByOrderByCreatedAtDesc(any());
    }

    // ── getObservations ───────────────────────────────────────────────────────

    @Test
    void getObservations_allEntries_whenUnresolvedOnlyFalse() {
        ObservationEntry e = buildObservation(1L, false);
        Page<ObservationEntry> page = new PageImpl<>(List.of(e));
        when(observationRepo.findAllByOrderByDetectedAtDesc(any(Pageable.class))).thenReturn(page);

        PageResponse<ObservationEntryDto> result = controller.getObservations(0, 10, false);

        assertEquals(1, result.getContent().size());
        verify(observationRepo).findAllByOrderByDetectedAtDesc(any());
    }

    @Test
    void getObservations_unresolvedOnly() {
        ObservationEntry e = buildObservation(2L, false);
        Page<ObservationEntry> page = new PageImpl<>(List.of(e));
        when(observationRepo.findByResolvedFalseOrderByDetectedAtDesc(any(Pageable.class)))
                .thenReturn(page);

        PageResponse<ObservationEntryDto> result = controller.getObservations(0, 10, true);

        assertEquals(1, result.getContent().size());
        verify(observationRepo).findByResolvedFalseOrderByDetectedAtDesc(any());
    }

    // ── getUnresolvedCount ────────────────────────────────────────────────────

    @Test
    void getUnresolvedCount_returnsCorrectNumber() {
        when(observationRepo.countByResolvedFalse()).thenReturn(3L);

        var result = controller.getUnresolvedCount();

        assertEquals(3L, result.get("unresolved"));
    }

    // ── resolveObservation ────────────────────────────────────────────────────

    @Test
    void resolveObservation_setsResolvedFields() {
        ObservationEntry e = buildObservation(10L, false);
        when(observationRepo.findById(10L)).thenReturn(Optional.of(e));
        when(observationRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ObservationEntryDto dto = controller.resolveObservation(10L);

        assertTrue(e.isResolved());
        assertNotNull(e.getResolvedAt());
        assertEquals("admin", e.getResolvedBy()); // ctxUsername from admin context
        assertTrue(dto.resolved());
        verify(auditLogService).logAuth(anyLong(), anyString(), anyString(),
                eq(AuditAction.ADMIN_RESOLVE_OBSERVATION), anyString(), isNull(), eq(true));
    }

    @Test
    void resolveObservation_throwsNotFound_whenMissing() {
        when(observationRepo.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.resolveObservation(999L));
        assertEquals(404, ex.getStatusCode().value());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void setAdminContext() {
        AuditContextHolder.AuditContext ctx = new AuditContextHolder.AuditContext();
        ctx.setUserId(99L);
        ctx.setUsername("admin");
        ctx.setUserRole("ADMIN");
        ctx.setIpAddress(null);
        AuditContextHolder.set(ctx);
    }

    private AuditLog buildLog(Long id, AuditAction action) {
        return AuditLog.builder()
                .id(id)
                .action(action)
                .success(true)
                .createdAt(Instant.now())
                .build();
    }

    private ObservationEntry buildObservation(Long id, boolean resolved) {
        return ObservationEntry.builder()
                .id(id)
                .userId(1L)
                .reason("BRUTE_FORCE_LOGIN")
                .severity("HIGH")
                .detectedAt(Instant.now())
                .occurrenceCount(1)
                .resolved(resolved)
                .build();
    }
}
