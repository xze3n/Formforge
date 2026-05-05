package com.formforge.service;

import com.formforge.model.AuditAction;
import com.formforge.model.AuditLog;
import com.formforge.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock private AuditLogRepository auditLogRepository;
    @Mock private ThreatDetectionService threatDetectionService;

    private AuditLogService service;

    @BeforeEach
    void setUp() {
        service = new AuditLogService(auditLogRepository, threatDetectionService);
    }

    @Test
    void log_savesEntry_andCallsAnalyze_whenUserIdNotNull() {
        when(auditLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.log(1L, "alice", "USER", AuditAction.CREATE_APPLICATION,
                "Application", "42", "detail", "1.2.3.4", true);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog saved = captor.getValue();
        assertEquals(1L, saved.getUserId());
        assertEquals("alice", saved.getUsername());
        assertEquals("USER", saved.getUserRole());
        assertEquals(AuditAction.CREATE_APPLICATION, saved.getAction());
        assertEquals("Application", saved.getResourceType());
        assertEquals("42", saved.getResourceId());
        assertEquals("detail", saved.getDetails());
        assertEquals("1.2.3.4", saved.getIpAddress());
        assertTrue(saved.isSuccess());

        verify(threatDetectionService).analyzeAsync(1L, "alice", AuditAction.CREATE_APPLICATION, true);
    }

    @Test
    void log_doesNotCallAnalyze_whenUserIdIsNull() {
        when(auditLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.log(null, "unknown@example.com", null, AuditAction.LOGIN_FAILURE,
                null, null, "Unknown email", "2.3.4.5", false);

        verify(auditLogRepository).save(any());
        verifyNoInteractions(threatDetectionService);
    }

    @Test
    void logAuth_delegatesToLog() {
        when(auditLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.logAuth(5L, "bob", "ADMIN", AuditAction.ADMIN_VIEW_LOGS,
                "viewed logs", "9.9.9.9", true);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog saved = captor.getValue();
        assertEquals(5L, saved.getUserId());
        assertEquals(AuditAction.ADMIN_VIEW_LOGS, saved.getAction());
        assertNull(saved.getResourceType());
        assertNull(saved.getResourceId());
        assertEquals("viewed logs", saved.getDetails());

        verify(threatDetectionService).analyzeAsync(5L, "bob", AuditAction.ADMIN_VIEW_LOGS, true);
    }

    @Test
    void logAuth_withNullUserId_doesNotCallAnalyze() {
        when(auditLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.logAuth(null, "x@x.com", null, AuditAction.LOGIN_FAILURE,
                null, "unknown", false);

        verify(auditLogRepository).save(any());
        verifyNoInteractions(threatDetectionService);
    }
}
