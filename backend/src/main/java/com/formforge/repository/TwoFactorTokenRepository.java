package com.formforge.repository;

import com.formforge.model.TwoFactorToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface TwoFactorTokenRepository extends JpaRepository<TwoFactorToken, Long> {

    Optional<TwoFactorToken> findByToken(String token);

    /** Deletes expired tokens to keep the table clean. */
    @Modifying
    @Query("DELETE FROM TwoFactorToken t WHERE t.expiresAt < :now")
    void deleteExpired(@Param("now") Instant now);
}
