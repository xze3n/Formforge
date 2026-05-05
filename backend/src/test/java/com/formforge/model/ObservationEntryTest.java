package com.formforge.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ObservationEntryTest {

    @Test
    void builder_setsAllFields() {
        Instant detected = Instant.now();
        ObservationEntry e = ObservationEntry.builder()
                .id(1L)
                .userId(10L)
                .username("eve")
                .reason("BRUTE_FORCE_LOGIN")
                .severity("HIGH")
                .detectedAt(detected)
                .resolved(false)
                .resolvedAt(null)
                .resolvedBy(null)
                .triggerAction("LOGIN_FAILURE")
                .occurrenceCount(3)
                .build();

        assertEquals(1L, e.getId());
        assertEquals(10L, e.getUserId());
        assertEquals("eve", e.getUsername());
        assertEquals("BRUTE_FORCE_LOGIN", e.getReason());
        assertEquals("HIGH", e.getSeverity());
        assertEquals(detected, e.getDetectedAt());
        assertFalse(e.isResolved());
        assertNull(e.getResolvedAt());
        assertNull(e.getResolvedBy());
        assertEquals("LOGIN_FAILURE", e.getTriggerAction());
        assertEquals(3, e.getOccurrenceCount());
    }

    @Test
    void noArgsConstructor_andSetters() {
        ObservationEntry e = new ObservationEntry();
        e.setId(2L);
        e.setUserId(5L);
        e.setReason("RAPID_DELETION");
        e.setSeverity("HIGH");
        e.setResolved(true);
        e.setOccurrenceCount(2);

        assertEquals(2L, e.getId());
        assertEquals(5L, e.getUserId());
        assertEquals("RAPID_DELETION", e.getReason());
        assertTrue(e.isResolved());
        assertEquals(2, e.getOccurrenceCount());
    }

    @Test
    void prePersist_setsDetectedAt_whenNull() {
        ObservationEntry e = new ObservationEntry();
        assertNull(e.getDetectedAt());
        e.prePersist();
        assertNotNull(e.getDetectedAt());
    }

    @Test
    void prePersist_doesNotOverwrite_existingDetectedAt() {
        Instant fixed = Instant.parse("2021-06-01T12:00:00Z");
        ObservationEntry e = ObservationEntry.builder()
                .detectedAt(fixed)
                .build();
        e.prePersist();
        assertEquals(fixed, e.getDetectedAt());
    }
}
