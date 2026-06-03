package com.formforge.service;

import com.formforge.model.AuditAction;
import com.formforge.model.ObservationEntry;
import com.formforge.repository.AuditLogRepository;
import com.formforge.repository.ObservationEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ThreatDetectionServiceTest {

    @Mock private AuditLogRepository auditLogRepository;
    @Mock private ObservationEntryRepository observationEntryRepository;
    @Mock private OllamaService ollamaService;

    private ThreatDetectionService service;

    @BeforeEach
    void setUp() {
        service = new ThreatDetectionService(auditLogRepository, observationEntryRepository, ollamaService);
    }

    // ── analyzeAsync runs synchronously in unit tests (no Spring proxy) ─────

    @Test
    void bruteForceLogin_flagsEntry_whenThresholdReached() {
        when(auditLogRepository.countByUserIdAndActionAndSuccessAndCreatedAtAfter(
                eq(1L), eq(AuditAction.LOGIN_FAILURE), eq(false), any(Instant.class)))
                .thenReturn(5L);
        stubNoOtherCounts();
        when(observationEntryRepository.findByUserIdAndReason(1L, "BRUTE_FORCE_LOGIN"))
                .thenReturn(Optional.empty());
        when(observationEntryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.analyzeAsync(1L, "alice", AuditAction.LOGIN_FAILURE, false);

        ArgumentCaptor<ObservationEntry> captor = ArgumentCaptor.forClass(ObservationEntry.class);
        verify(observationEntryRepository, atLeastOnce()).save(captor.capture());
        ObservationEntry e = captor.getAllValues().get(0);
        assertEquals("BRUTE_FORCE_LOGIN", e.getReason());
        assertEquals("HIGH", e.getSeverity());
        assertEquals("LOGIN_FAILURE", e.getTriggerAction());
        assertEquals(1, e.getOccurrenceCount());
    }

    @Test
    void bruteForceLogin_doesNotFlag_belowThreshold() {
        when(auditLogRepository.countByUserIdAndActionAndSuccessAndCreatedAtAfter(
                eq(1L), eq(AuditAction.LOGIN_FAILURE), eq(false), any()))
                .thenReturn(4L);
        stubNoOtherCounts();

        service.analyzeAsync(1L, "alice", AuditAction.LOGIN_FAILURE, false);

        verify(observationEntryRepository, never()).save(any());
    }

    @Test
    void rapidDeletion_flagsEntry_whenThresholdReached() {
        when(auditLogRepository.countByUserIdAndActionAndSuccessAndCreatedAtAfter(
                anyLong(), any(), anyBoolean(), any())).thenReturn(0L);
        when(auditLogRepository.countByUserIdAndActionAndCreatedAtAfter(
                eq(2L), eq(AuditAction.DELETE_APPLICATION), any(Instant.class)))
                .thenReturn(3L);
        when(auditLogRepository.countByUserIdAndActionInAndCreatedAtAfter(
                anyLong(), anyList(), any())).thenReturn(0L);
        when(observationEntryRepository.findByUserIdAndReason(2L, "RAPID_DELETION"))
                .thenReturn(Optional.empty());
        when(observationEntryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.analyzeAsync(2L, "bob", AuditAction.DELETE_APPLICATION, true);

        ArgumentCaptor<ObservationEntry> captor = ArgumentCaptor.forClass(ObservationEntry.class);
        verify(observationEntryRepository, atLeastOnce()).save(captor.capture());
        assertTrue(captor.getAllValues().stream()
                .anyMatch(e -> "RAPID_DELETION".equals(e.getReason())));
    }

    @Test
    void rapidDeletion_doesNotFlag_belowThreshold() {
        when(auditLogRepository.countByUserIdAndActionAndSuccessAndCreatedAtAfter(
                anyLong(), any(), anyBoolean(), any())).thenReturn(0L);
        when(auditLogRepository.countByUserIdAndActionAndCreatedAtAfter(
                anyLong(), eq(AuditAction.DELETE_APPLICATION), any())).thenReturn(2L);
        when(auditLogRepository.countByUserIdAndActionInAndCreatedAtAfter(
                anyLong(), anyList(), any())).thenReturn(0L);

        service.analyzeAsync(2L, "bob", AuditAction.DELETE_APPLICATION, true);

        verify(observationEntryRepository, never()).save(any());
    }

    @Test
    void massEnumeration_flagsEntry_whenThresholdReached() {
        when(auditLogRepository.countByUserIdAndActionAndSuccessAndCreatedAtAfter(
                anyLong(), any(), anyBoolean(), any())).thenReturn(0L);
        when(auditLogRepository.countByUserIdAndActionAndCreatedAtAfter(
                eq(3L), eq(AuditAction.DELETE_APPLICATION), any())).thenReturn(0L);
        when(auditLogRepository.countByUserIdAndActionAndCreatedAtAfter(
                eq(3L), eq(AuditAction.READ_APPLICATIONS), any())).thenReturn(30L);
        when(auditLogRepository.countByUserIdAndActionInAndCreatedAtAfter(
                anyLong(), anyList(), any())).thenReturn(0L);
        when(observationEntryRepository.findByUserIdAndReason(3L, "MASS_ENUMERATION"))
                .thenReturn(Optional.empty());
        when(observationEntryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.analyzeAsync(3L, "scraper", AuditAction.READ_APPLICATIONS, true);

        ArgumentCaptor<ObservationEntry> captor = ArgumentCaptor.forClass(ObservationEntry.class);
        verify(observationEntryRepository, atLeastOnce()).save(captor.capture());
        assertTrue(captor.getAllValues().stream()
                .anyMatch(e -> "MASS_ENUMERATION".equals(e.getReason())));
    }

    @Test
    void massMutation_flagsEntry_whenThresholdReached() {
        when(auditLogRepository.countByUserIdAndActionAndSuccessAndCreatedAtAfter(
                anyLong(), any(), anyBoolean(), any())).thenReturn(0L);
        when(auditLogRepository.countByUserIdAndActionAndCreatedAtAfter(
                anyLong(), any(), any())).thenReturn(0L);
        when(auditLogRepository.countByUserIdAndActionInAndCreatedAtAfter(
                eq(4L), anyList(), any())).thenReturn(10L);
        when(observationEntryRepository.findByUserIdAndReason(4L, "MASS_MUTATION"))
                .thenReturn(Optional.empty());
        when(observationEntryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.analyzeAsync(4L, "bot", AuditAction.CREATE_APPLICATION, true);

        ArgumentCaptor<ObservationEntry> captor = ArgumentCaptor.forClass(ObservationEntry.class);
        verify(observationEntryRepository, atLeastOnce()).save(captor.capture());
        assertTrue(captor.getAllValues().stream()
                .anyMatch(e -> "MASS_MUTATION".equals(e.getReason())));
    }

    @Test
    void repeatedFailures_flagsEntry_whenThresholdReached_andActionFailed() {
        when(auditLogRepository.countByUserIdAndActionAndSuccessAndCreatedAtAfter(
                eq(5L), eq(AuditAction.DELETE_APPLICATION), eq(false), any())).thenReturn(5L);
        // brute force check (LOGIN_FAILURE, false) returns 0 for this user
        when(auditLogRepository.countByUserIdAndActionAndSuccessAndCreatedAtAfter(
                eq(5L), eq(AuditAction.LOGIN_FAILURE), eq(false), any())).thenReturn(0L);
        when(auditLogRepository.countByUserIdAndActionAndCreatedAtAfter(
                anyLong(), any(), any())).thenReturn(0L);
        when(auditLogRepository.countByUserIdAndActionInAndCreatedAtAfter(
                anyLong(), anyList(), any())).thenReturn(0L);
        String reason = "REPEATED_FAILURES_DELETE_APPLICATION";
        when(observationEntryRepository.findByUserIdAndReason(5L, reason))
                .thenReturn(Optional.empty());
        when(observationEntryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.analyzeAsync(5L, "carol", AuditAction.DELETE_APPLICATION, false);

        ArgumentCaptor<ObservationEntry> captor = ArgumentCaptor.forClass(ObservationEntry.class);
        verify(observationEntryRepository, atLeastOnce()).save(captor.capture());
        assertTrue(captor.getAllValues().stream()
                .anyMatch(e -> e.getReason().startsWith("REPEATED_FAILURES_") && "LOW".equals(e.getSeverity())));
    }

    @Test
    void repeatedFailures_notChecked_whenActionSucceeded() {
        stubNoOtherCounts();
        // success=true → checkRepeatedFailures is NOT called
        service.analyzeAsync(6L, "dave", AuditAction.READ_APPLICATIONS, true);
        // no observation saved
        verify(observationEntryRepository, never()).save(any());
    }

    @Test
    void flag_incrementsOccurrenceCount_whenEntryAlreadyExists() {
        ObservationEntry existing = ObservationEntry.builder()
                .id(100L)
                .userId(1L)
                .username("alice")
                .reason("BRUTE_FORCE_LOGIN")
                .severity("HIGH")
                .occurrenceCount(2)
                .resolved(true)
                .build();

        when(auditLogRepository.countByUserIdAndActionAndSuccessAndCreatedAtAfter(
                eq(1L), eq(AuditAction.LOGIN_FAILURE), eq(false), any())).thenReturn(5L);
        stubNoOtherCounts();
        when(observationEntryRepository.findByUserIdAndReason(1L, "BRUTE_FORCE_LOGIN"))
                .thenReturn(Optional.of(existing));
        when(observationEntryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.analyzeAsync(1L, "alice", AuditAction.LOGIN_FAILURE, false);

        verify(observationEntryRepository).save(existing);
        assertEquals(3, existing.getOccurrenceCount());
        assertFalse(existing.isResolved()); // re-surfaced
    }

    @Test
    void analyzeAsync_catchesException_withoutPropagating() {
        when(auditLogRepository.countByUserIdAndActionAndSuccessAndCreatedAtAfter(
                anyLong(), any(), anyBoolean(), any()))
                .thenThrow(new RuntimeException("DB error"));

        assertDoesNotThrow(() ->
                service.analyzeAsync(1L, "alice", AuditAction.LOGIN_FAILURE, false));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void stubNoOtherCounts() {
        lenient().when(auditLogRepository.countByUserIdAndActionAndCreatedAtAfter(
                anyLong(), any(), any())).thenReturn(0L);
        lenient().when(auditLogRepository.countByUserIdAndActionInAndCreatedAtAfter(
                anyLong(), anyList(), any())).thenReturn(0L);
    }
}
