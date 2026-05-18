package com.formforge.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private static final String SECRET           = "TestSecret-Min32Chars-ForHmacSha256Key!";
    private static final long   EXPIRY_MINUTES   = 30;

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(SECRET, EXPIRY_MINUTES);
    }

    // ── generateToken ─────────────────────────────────────────────────────

    @Test
    void generateToken_returnsNonNullString() {
        String token = jwtUtil.generateToken(1L, "alice", "USER");
        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void generateToken_producesValidJwt_withThreeParts() {
        String token = jwtUtil.generateToken(1L, "alice", "USER");
        // A valid JWT has header.payload.signature
        assertEquals(3, token.split("\\.").length);
    }

    // ── validateAndExtract ────────────────────────────────────────────────

    @Test
    void validateAndExtract_validToken_returnsClaims() {
        String token = jwtUtil.generateToken(42L, "bob", "ADMIN");

        Claims claims = jwtUtil.validateAndExtract(token);

        assertNotNull(claims);
        assertEquals("42", claims.getSubject());
        assertEquals("bob",   claims.get("username", String.class));
        assertEquals("ADMIN", claims.get("role",     String.class));
    }

    @Test
    void validateAndExtract_invalidSignature_throwsJwtException() {
        String token = jwtUtil.generateToken(1L, "alice", "USER");
        // Tamper with the signature part
        String tampered = token.substring(0, token.lastIndexOf('.') + 1) + "invalidsignature";
        assertThrows(JwtException.class, () -> jwtUtil.validateAndExtract(tampered));
    }

    @Test
    void validateAndExtract_garbage_throwsException() {
        assertThrows(Exception.class, () -> jwtUtil.validateAndExtract("not.a.jwt"));
    }

    @Test
    void validateAndExtract_expiredToken_throwsJwtException() {
        // Create a JwtUtil with 0-minute expiry → token is immediately expired
        JwtUtil expiredUtil = new JwtUtil(SECRET, 0L);
        String token = expiredUtil.generateToken(1L, "alice", "USER");
        // The token should be expired (or expiry == issued)
        // Parsing it with a 30-min util should still fail as the exp is in the past
        assertThrows(JwtException.class, () -> new JwtUtil(SECRET, 30).validateAndExtract(token));
    }

    // ── extraction helpers ────────────────────────────────────────────────

    @Test
    void extractUserId_returnsCorrectId() {
        String token  = jwtUtil.generateToken(99L, "carol", "USER");
        Claims claims = jwtUtil.validateAndExtract(token);
        assertEquals(99L, jwtUtil.extractUserId(claims));
    }

    @Test
    void extractUsername_returnsCorrectUsername() {
        String token  = jwtUtil.generateToken(1L, "dave", "USER");
        Claims claims = jwtUtil.validateAndExtract(token);
        assertEquals("dave", jwtUtil.extractUsername(claims));
    }

    @Test
    void extractRole_returnsCorrectRole() {
        String token  = jwtUtil.generateToken(1L, "eve", "ADMIN");
        Claims claims = jwtUtil.validateAndExtract(token);
        assertEquals("ADMIN", jwtUtil.extractRole(claims));
    }

    // ── constructor validation ────────────────────────────────────────────

    @Test
    void constructor_shortSecret_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new JwtUtil("short", 30));
    }

    @Test
    void constructor_blankSecret_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new JwtUtil("   ", 30));
    }
}
