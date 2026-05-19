package com.formforge.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Stateless JWT helper: generates and validates HMAC-SHA256 signed tokens.
 *
 * <p>Tokens carry the user's id, username, and role and expire after
 * {@code jwt.expiration-minutes} minutes (default 30). The frontend must
 * include the token as a {@code Authorization: Bearer <token>} header on
 * every protected request; no server-side token store is needed.
 */
@Component
public class JwtUtil {

    private final SecretKey signingKey;
    private final long     expirationMs;

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-minutes:30}") long expirationMinutes) {

        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("jwt.secret must not be blank");
        }
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException(
                    "jwt.secret must be at least 32 characters (256 bits) for HMAC-SHA256");
        }
        this.signingKey   = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expirationMinutes * 60_000L;
    }

    /** Creates a signed JWT carrying the user's id, username, role, and permissions. */
    public String generateToken(Long userId, String username, String role, Set<String> permissions) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        String permsClaim = (permissions != null && !permissions.isEmpty())
                ? String.join(",", permissions)
                : "";

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("role", role)
                .claim("permissions", permsClaim)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    /**
     * Parses and validates a token. Returns its claims on success.
     *
     * @throws JwtException if the token is invalid or expired.
     */
    public Claims validateAndExtract(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** Extracts user id (subject) without re-validating – only call after validateAndExtract. */
    public Long extractUserId(Claims claims) {
        return Long.parseLong(claims.getSubject());
    }

    public String extractUsername(Claims claims) {
        return claims.get("username", String.class);
    }

    public String extractRole(Claims claims) {
        return claims.get("role", String.class);
    }

    /** Returns the set of permission names embedded in the token (e.g. "READ_APPLICATIONS"). */
    public Set<String> extractPermissions(Claims claims) {
        String raw = claims.get("permissions", String.class);
        if (raw == null || raw.isBlank()) return Set.of();
        return Set.of(raw.split(",")).stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }
}
