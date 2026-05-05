package com.formforge.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Reads X-User-Id / X-User-Name / X-User-Role headers forwarded by the React
 * frontend and populates {@link AuditContextHolder} for the duration of the request.
 */
@Component
@Order(1)
public class AuditFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {
        try {
            String userIdHeader = request.getHeader("X-User-Id");
            if (userIdHeader != null && !userIdHeader.isBlank()) {
                AuditContextHolder.AuditContext ctx = new AuditContextHolder.AuditContext();
                try {
                    ctx.setUserId(Long.parseLong(userIdHeader.trim()));
                } catch (NumberFormatException ignored) { /* malformed – treat as anonymous */ }
                ctx.setUsername(request.getHeader("X-User-Name"));
                ctx.setUserRole(request.getHeader("X-User-Role"));
                ctx.setIpAddress(extractIp(request));
                AuditContextHolder.set(ctx);
            }
            chain.doFilter(request, response);
        } finally {
            AuditContextHolder.clear();
        }
    }

    private String extractIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}
