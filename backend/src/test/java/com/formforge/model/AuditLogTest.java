package com.formforge.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class AuditLogTest {

    @Test
    void builder_setsAllFields() {
        Instant now = Instant.now();
        AuditLog log = AuditLog.builder()
                .id(1L)
                .userId(5L)
                .username("alice")
                .userRole("ADMIN")
                .action(AuditAction.DELETE_APPLICATION)
                .resourceType("Application")
                .resourceId("99")
                .details("test detail")
                .ipAddress("1.2.3.4")
                .success(false)
                .createdAt(now)
                .build();

        assertEquals(1L,   log.getId());
        assertEquals(5L,   log.getUserId());
        assertEquals("alice", log.getUsername());
        assertEquals("ADMIN", log.getUserRole());
        assertEquals(AuditAction.DELETE_APPLICATION, log.getAction());
        assertEquals("Application", log.getResourceType());
        assertEquals("99", log.getResourceId());
        assertEquals("test detail", log.getDetails());
        assertEquals("1.2.3.4", log.getIpAddress());
        assertFalse(log.isSuccess());
        assertEquals(now, log.getCreatedAt());
    }

    @Test
    void noArgsConstructor_andSetters() {
        AuditLog log = new AuditLog();
        log.setId(2L);
        log.setUserId(3L);
        log.setAction(AuditAction.LOGIN_SUCCESS);
        log.setSuccess(true);

        assertEquals(2L, log.getId());
        assertEquals(3L, log.getUserId());
        assertEquals(AuditAction.LOGIN_SUCCESS, log.getAction());
        assertTrue(log.isSuccess());
    }

    @Test
    void prePersist_setsCreatedAt_whenNull() {
        AuditLog log = new AuditLog();
        assertNull(log.getCreatedAt());
        // Invoke lifecycle callback
        log.prePersist();
        assertNotNull(log.getCreatedAt());
    }

    @Test
    void prePersist_doesNotOverwrite_existingCreatedAt() {
        Instant fixed = Instant.parse("2020-01-01T00:00:00Z");
        AuditLog log = AuditLog.builder().createdAt(fixed).build();
        log.prePersist();
        assertEquals(fixed, log.getCreatedAt());
    }

    @Test
    void allArgsConstructor() {
        Instant now = Instant.now();
        AuditLog log = new AuditLog(10L, 1L, "bob", "USER", AuditAction.REGISTER,
                null, null, null, "127.0.0.1", true, now);
        assertEquals(10L, log.getId());
        assertEquals("bob", log.getUsername());
        assertEquals(AuditAction.REGISTER, log.getAction());
    }
}
