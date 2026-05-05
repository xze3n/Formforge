package com.formforge.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "observation_list")
public class ObservationEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    private String username;

    @Column(nullable = false)
    private String reason;

    @Column(nullable = false)
    private String severity; // LOW | MEDIUM | HIGH | CRITICAL

    @Column(nullable = false)
    private Instant detectedAt;

    private boolean resolved = false;
    private Instant resolvedAt;
    private String resolvedBy;
    private String triggerAction;

    @Column(nullable = false)
    private int occurrenceCount = 1;

    @PrePersist
    void prePersist() {
        if (detectedAt == null) detectedAt = Instant.now();
    }
}
