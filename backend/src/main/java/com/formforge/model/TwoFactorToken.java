package com.formforge.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Short-lived, single-use token used as the second authentication factor.
 * After the user passes the password check, a token is generated and logged
 * to the server console (simulating an e-mail send). The user must supply the
 * token to {@code POST /api/auth/verify-2fa} to receive their JWT.
 */
@Entity
@Table(name = "two_factor_tokens")
@Data
@NoArgsConstructor
public class TwoFactorToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** URL-safe UUID token — 36 chars. */
    @Column(nullable = false, unique = true, length = 64)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean used = false;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
