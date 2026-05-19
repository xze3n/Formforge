package com.formforge.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuditFilterTest {

    private final AuditFilter filter = new AuditFilter();

    @AfterEach
    void cleanup() {
        AuditContextHolder.clear();
        SecurityContextHolder.clearContext();
    }

    private void setAuthentication(Long userId, String username, String role) {
        var auth = new UsernamePasswordAuthenticationToken(
                userId, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        auth.setDetails(username);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void setsContext_fromSecurityContext() throws Exception {
        setAuthentication(10L, "bob", "USER");

        MockHttpServletRequest req = new MockHttpServletRequest();
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
        setAuthentication(5L, "alice", "ADMIN");

        MockHttpServletRequest req = new MockHttpServletRequest();
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
    void doesNotSetContext_whenSecurityContextEmpty() throws Exception {
        // No authentication set
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
    void doesNotSetContext_forAnonymousAuthentication() throws Exception {
        var anonAuth = new AnonymousAuthenticationToken(
                "anon-key", "anonymousUser",
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        SecurityContextHolder.getContext().setAuthentication(anonAuth);

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
    void clearsContext_evenWhenChainThrows() throws Exception {
        setAuthentication(1L, "user", "USER");

        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        doThrow(new IOException("simulated failure")).when(chain).doFilter(any(), any());

        assertThrows(IOException.class, () -> filter.doFilter(req, res, chain));
        assertNull(AuditContextHolder.get());
    }
}
