package com.formforge.dto;

import com.formforge.model.AuditAction;
import com.formforge.model.AuditLog;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class AuditLogDtoTest {

    @Test
    void from_mapsAllFields() {
        Instant now = Instant.now();
        AuditLog log = AuditLog.builder()
                .id(7L)
                .userId(3L)
                .username("alice")
                .userRole("ADMIN")
                .action(AuditAction.CREATE_APPLICATION)
                .resourceType("Application")
                .resourceId("42")
                .details("some details")
                .ipAddress("10.0.0.1")
                .success(true)
                .createdAt(now)
                .build();

        AuditLogDto dto = AuditLogDto.from(log);

        assertEquals(7L,   dto.id());
        assertEquals(3L,   dto.userId());
        assertEquals("alice", dto.username());
        assertEquals("ADMIN", dto.userRole());
        assertEquals("CREATE_APPLICATION", dto.action());
        assertEquals("Application", dto.resourceType());
        assertEquals("42", dto.resourceId());
        assertEquals("some details", dto.details());
        assertEquals("10.0.0.1", dto.ipAddress());
        assertTrue(dto.success());
        assertEquals(now, dto.createdAt());
    }

    @Test
    void from_handlesNullOptionalFields() {
        AuditLog log = AuditLog.builder()
                .id(1L)
                .action(AuditAction.LOGIN_SUCCESS)
                .success(true)
                .createdAt(Instant.now())
                .build();

        AuditLogDto dto = AuditLogDto.from(log);

        assertNull(dto.userId());
        assertNull(dto.username());
        assertNull(dto.userRole());
        assertNull(dto.resourceType());
        assertNull(dto.resourceId());
        assertNull(dto.details());
        assertNull(dto.ipAddress());
    }

    @Test
    void from_successFalse() {
        AuditLog log = AuditLog.builder()
                .id(2L)
                .action(AuditAction.LOGIN_FAILURE)
                .success(false)
                .createdAt(Instant.now())
                .build();

        AuditLogDto dto = AuditLogDto.from(log);
        assertFalse(dto.success());
        assertEquals("LOGIN_FAILURE", dto.action());
    }
}
