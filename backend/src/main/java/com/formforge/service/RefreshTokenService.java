package com.formforge.service;

import com.formforge.model.RefreshToken;
import com.formforge.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Manages long-lived refresh tokens that are stored in the database.
 * Access tokens remain stateless JWTs; refresh tokens enable logout and rotation.
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    @Value("${jwt.refresh-expiration-days:7}")
    private long refreshExpirationDays;

    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * Creates (and persists) a new refresh token for {@code userId}.
     * Any previous tokens for the user are kept; logout / rotation revokes individually.
     */
    @Transactional
    public RefreshToken createRefreshToken(Long userId) {
        RefreshToken token = new RefreshToken();
        token.setUserId(userId);
        token.setToken(UUID.randomUUID().toString());
        token.setExpiresAt(Instant.now().plus(refreshExpirationDays, ChronoUnit.DAYS));
        return refreshTokenRepository.save(token);
    }

    /**
     * Looks up and validates a refresh token string.
     * Throws 401 if the token is unknown, revoked, or expired.
     */
    @Transactional(readOnly = true)
    public RefreshToken validateRefreshToken(String tokenStr) {
        RefreshToken token = refreshTokenRepository.findByToken(tokenStr)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Invalid refresh token"));
        if (token.isRevoked()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token has been revoked");
        }
        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token has expired");
        }
        return token;
    }

    /** Marks a single refresh token as revoked (used on logout or rotation). */
    @Transactional
    public void revokeToken(String tokenStr) {
        refreshTokenRepository.findByToken(tokenStr).ifPresent(t -> {
            t.setRevoked(true);
            refreshTokenRepository.save(t);
        });
    }

    /** Deletes all refresh tokens for a user (used on password reset). */
    @Transactional
    public void revokeAllForUser(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }
}
