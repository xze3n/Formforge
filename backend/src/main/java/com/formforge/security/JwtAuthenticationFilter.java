package com.formforge.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Reads the {@code Authorization: Bearer <token>} header on every request,
 * validates the JWT and – if valid – populates the Spring Security context
 * so that subsequent security rules can inspect the authenticated principal.
 *
 * <p>Requests without a token (e.g. /api/auth/**) pass through untouched;
 * Spring Security's {@code authorizeHttpRequests} rules decide whether
 * they are allowed.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest  request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain         chain) throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        try {
            Claims claims   = jwtUtil.validateAndExtract(token);
            String username = jwtUtil.extractUsername(claims);
            String role     = jwtUtil.extractRole(claims);
            Long   userId   = jwtUtil.extractUserId(claims);

            // Attach authority e.g. "ROLE_USER" or "ROLE_ADMIN"
            var authority = new SimpleGrantedAuthority("ROLE_" + role);
            var auth      = new UsernamePasswordAuthenticationToken(
                                userId, null, List.of(authority));
            auth.setDetails(username);

            SecurityContextHolder.getContext().setAuthentication(auth);

        } catch (JwtException | IllegalArgumentException e) {
            // Invalid or expired token – clear context, let security rules reject it
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Invalid or expired token\"}");
            return;
        }

        chain.doFilter(request, response);
    }
}
