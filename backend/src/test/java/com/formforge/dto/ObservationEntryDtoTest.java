package com.formforge.dto;

import com.formforge.model.ObservationEntry;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ObservationEntryDtoTest {

    @Test
    void from_mapsAllFields() {
        Instant detected = Instant.now();
        Instant resolved = detected.plusSeconds(3600);
        ObservationEntry entry = ObservationEntry.builder()
                .id(5L)
                .userId(10L)
                .username("eve")
                .reason("BRUTE_FORCE_LOGIN")
                .severity("HIGH")
                .detectedAt(detected)
                .resolved(true)
                .resolvedAt(resolved)
                .resolvedBy("admin")
                .triggerAction("LOGIN_FAILURE")
                .occurrenceCount(7)
                .build();

        ObservationEntryDto dto = ObservationEntryDto.from(entry);

        assertEquals(5L,   dto.id());
        assertEquals(10L,  dto.userId());
        assertEquals("eve", dto.username());
        assertEquals("BRUTE_FORCE_LOGIN", dto.reason());
        assertEquals("HIGH", dto.severity());
        assertEquals(detected, dto.detectedAt());
        assertTrue(dto.resolved());
        assertEquals(resolved, dto.resolvedAt());
        assertEquals("admin", dto.resolvedBy());
        assertEquals("LOGIN_FAILURE", dto.triggerAction());
        assertEquals(7, dto.occurrenceCount());
    }

    @Test
    void from_handlesNullOptionalFields() {
        ObservationEntry entry = ObservationEntry.builder()
                .id(1L)
                .userId(2L)
                .reason("RAPID_DELETION")
                .severity("HIGH")
                .detectedAt(Instant.now())
                .occurrenceCount(1)
                .build();

        ObservationEntryDto dto = ObservationEntryDto.from(entry);

        assertFalse(dto.resolved());
        assertNull(dto.resolvedAt());
        assertNull(dto.resolvedBy());
    }
}
