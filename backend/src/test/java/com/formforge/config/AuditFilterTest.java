package com.formforge.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuditFilterTest {

    private final AuditFilter filter = new AuditFilter();

    @AfterEach
    void cleanup() {
        AuditContextHolder.clear();
    }

    @Test
    void setsContext_withXffHeader() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-User-Id", "10");
        req.addHeader("X-User-Name", "bob");
        req.addHeader("X-User-Role", "USER");
        req.addHeader("X-Forwarded-For", "5.6.7.8, 9.10.11.12");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        AuditContextHolder.AuditContext[] captured = {null};
        doAnswer(inv -> {
            captured[0] = AuditContextHolder.get();
            return null;
        }).when(chain).doFilter(any(), any());

        filter.doFilter(req, res, chain);

        assertNotNull(captured[0]);
        assertEquals(10L, captured[0].getUserId());
        assertEquals("bob", captured[0].getUsername());
        assertEquals("USER", captured[0].getUserRole());
        assertEquals("5.6.7.8", captured[0].getIpAddress());
        // context cleared after filter
        assertNull(AuditContextHolder.get());
    }

    @Test
    void setsContext_withRemoteAddr_whenNoXff() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-User-Id", "5");
        req.setRemoteAddr("192.168.1.1");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        AuditContextHolder.AuditContext[] captured = {null};
        doAnswer(inv -> {
            captured[0] = AuditContextHolder.get();
            return null;
        }).when(chain).doFilter(any(), any());

        filter.doFilter(req, res, chain);

        assertEquals("192.168.1.1", captured[0].getIpAddress());
    }

    @Test
    void doesNotSetContext_whenUserIdHeaderMissing() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        AuditContextHolder.AuditContext[] captured = {null};
        doAnswer(inv -> {
            captured[0] = AuditContextHolder.get();
            return null;
        }).when(chain).doFilter(any(), any());

        filter.doFilter(req, res, chain);

        assertNull(captured[0]);
    }

    @Test
    void doesNotSetContext_whenUserIdHeaderBlank() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-User-Id", "   ");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        AuditContextHolder.AuditContext[] captured = {null};
        doAnswer(inv -> {
            captured[0] = AuditContextHolder.get();
            return null;
        }).when(chain).doFilter(any(), any());

        filter.doFilter(req, res, chain);

        assertNull(captured[0]);
    }

    @Test
    void malformedUserId_setsContextWithNullUserId() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-User-Id", "not-a-number");
        req.addHeader("X-User-Name", "bob");
        req.addHeader("X-User-Role", "USER");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        AuditContextHolder.AuditContext[] captured = {null};
        doAnswer(inv -> {
            captured[0] = AuditContextHolder.get();
            return null;
        }).when(chain).doFilter(any(), any());

        filter.doFilter(req, res, chain);

        assertNotNull(captured[0]);
        assertNull(captured[0].getUserId());
    }

    @Test
    void clearsContext_evenWhenChainThrows() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-User-Id", "1");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        doThrow(new IOException("simulated failure")).when(chain).doFilter(any(), any());

        assertThrows(IOException.class, () -> filter.doFilter(req, res, chain));
        assertNull(AuditContextHolder.get());
    }
}
