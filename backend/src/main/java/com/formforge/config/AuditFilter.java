package com.formforge.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Populates {@link AuditContextHolder} from the Spring Security context for the
 * duration of each request.
 *
 * <p>Spring Security's {@code FilterChainProxy} has order {@code -100}, so by the
 * time this filter (order {@code 1}) executes, the {@code JwtAuthenticationFilter}
 * has already validated the bearer token and set the authentication in the
 * {@code SecurityContextHolder}. Reading from the security context here avoids
 * trusting client-supplied headers.
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
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null
                    && auth.isAuthenticated()
                    && !(auth instanceof AnonymousAuthenticationToken)) {

                AuditContextHolder.AuditContext ctx = new AuditContextHolder.AuditContext();

                if (auth.getPrincipal() instanceof Long userId) {
                    ctx.setUserId(userId);
                }
                if (auth.getDetails() instanceof String username) {
                    ctx.setUsername(username);
                }

                String role = auth.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .filter(a -> a.startsWith("ROLE_"))
                        .map(a -> a.substring(5))
                        .findFirst()
                        .orElse("USER");
                ctx.setUserRole(role);
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
