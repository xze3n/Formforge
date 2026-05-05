package com.formforge.dto;

import com.formforge.model.ObservationEntry;

import java.time.Instant;

public record ObservationEntryDto(
        Long id,
        Long userId,
        String username,
        String reason,
        String severity,
        Instant detectedAt,
        boolean resolved,
        Instant resolvedAt,
        String resolvedBy,
        String triggerAction,
        int occurrenceCount
) {
    public static ObservationEntryDto from(ObservationEntry e) {
        return new ObservationEntryDto(
                e.getId(),
                e.getUserId(),
                e.getUsername(),
                e.getReason(),
                e.getSeverity(),
                e.getDetectedAt(),
                e.isResolved(),
                e.getResolvedAt(),
                e.getResolvedBy(),
                e.getTriggerAction(),
                e.getOccurrenceCount()
        );
    }
}
