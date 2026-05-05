package com.formforge.repository;

import com.formforge.model.ObservationEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ObservationEntryRepository extends JpaRepository<ObservationEntry, Long> {

    Page<ObservationEntry> findAllByOrderByDetectedAtDesc(Pageable pageable);

    Page<ObservationEntry> findByResolvedFalseOrderByDetectedAtDesc(Pageable pageable);

    Optional<ObservationEntry> findByUserIdAndReason(Long userId, String reason);

    long countByResolvedFalse();
}
