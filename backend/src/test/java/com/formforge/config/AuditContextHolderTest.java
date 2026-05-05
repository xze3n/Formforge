package com.formforge.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.*;

class AuditContextHolderTest {

    @AfterEach
    void cleanup() {
        AuditContextHolder.clear();
    }

    @Test
    void setAndGet_returnsSameContext() {
        AuditContextHolder.AuditContext ctx = new AuditContextHolder.AuditContext();
        ctx.setUserId(42L);
        ctx.setUsername("alice");
        ctx.setUserRole("ADMIN");
        ctx.setIpAddress("1.2.3.4");

        AuditContextHolder.set(ctx);
        AuditContextHolder.AuditContext result = AuditContextHolder.get();

        assertNotNull(result);
        assertEquals(42L, result.getUserId());
        assertEquals("alice", result.getUsername());
        assertEquals("ADMIN", result.getUserRole());
        assertEquals("1.2.3.4", result.getIpAddress());
    }

    @Test
    void get_returnsNull_whenNotSet() {
        assertNull(AuditContextHolder.get());
    }

    @Test
    void clear_removesContext() {
        AuditContextHolder.AuditContext ctx = new AuditContextHolder.AuditContext();
        AuditContextHolder.set(ctx);
        AuditContextHolder.clear();
        assertNull(AuditContextHolder.get());
    }

    @Test
    void auditContext_equals_hashCode() {
        AuditContextHolder.AuditContext a = new AuditContextHolder.AuditContext();
        a.setUserId(1L);
        a.setUsername("bob");
        AuditContextHolder.AuditContext b = new AuditContextHolder.AuditContext();
        b.setUserId(1L);
        b.setUsername("bob");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void auditContext_toString_isNotNull() {
        AuditContextHolder.AuditContext ctx = new AuditContextHolder.AuditContext();
        ctx.setUserId(5L);
        assertNotNull(ctx.toString());
    }

    @Test
    void privateConstructor_isCovered() throws Exception {
        Constructor<AuditContextHolder> ctor = AuditContextHolder.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        assertNotNull(ctor.newInstance());
    }
}
