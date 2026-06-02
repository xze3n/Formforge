package com.formforge.repository;

import com.formforge.model.ObservationEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ObservationEntryRepository extends JpaRepository<ObservationEntry, Long> {

    Page<ObservationEntry> findAllByOrderByDetectedAtDesc(Pageable pageable);

    Page<ObservationEntry> findByResolvedFalseOrderByDetectedAtDesc(Pageable pageable);

    Optional<ObservationEntry> findByUserIdAndReason(Long userId, String reason);

    long countByResolvedFalse();

    /**
     * Atomic upsert — safe under concurrent async threads.
     * On conflict (user_id, reason) increments the counter instead of failing.
     */
    @Modifying(clearAutomatically = true)
    @Query(nativeQuery = true, value = """
            INSERT INTO observation_list
                (user_id, username, reason, severity, trigger_action, details,
                 occurrence_count, detected_at, resolved)
            VALUES
                (:userId, :username, :reason, :severity, :triggerAction, :details,
                 1, now(), false)
            ON CONFLICT (user_id, reason) DO UPDATE SET
                occurrence_count = observation_list.occurrence_count + 1,
                detected_at      = now(),
                details          = EXCLUDED.details,
                resolved         = false
            """)
    void upsertObservation(@Param("userId")       Long   userId,
                           @Param("username")     String username,
                           @Param("reason")       String reason,
                           @Param("severity")     String severity,
                           @Param("triggerAction") String triggerAction,
                           @Param("details")      String details);
}
