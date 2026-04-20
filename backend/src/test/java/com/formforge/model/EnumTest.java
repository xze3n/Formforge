package com.formforge.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EnumTest {

    @Test
    void applicationTypeValues() {
        assertEquals("Merit", ApplicationType.MERIT.getValue());
        assertEquals("Social", ApplicationType.SOCIAL.getValue());
        assertEquals("Performance", ApplicationType.PERFORMANCE.getValue());
    }

    @Test
    void applicationTypeValuesCount() {
        assertEquals(3, ApplicationType.values().length);
    }

    @Test
    void applicationTypeValueOf() {
        assertEquals(ApplicationType.MERIT, ApplicationType.valueOf("MERIT"));
        assertEquals(ApplicationType.SOCIAL, ApplicationType.valueOf("SOCIAL"));
        assertEquals(ApplicationType.PERFORMANCE, ApplicationType.valueOf("PERFORMANCE"));
    }

    @Test
    void applicationStatusValues() {
        assertEquals("Draft", ApplicationStatus.DRAFT.getValue());
        assertEquals("Pending Action", ApplicationStatus.PENDING_ACTION.getValue());
        assertEquals("Approved", ApplicationStatus.APPROVED.getValue());
    }

    @Test
    void applicationStatusValuesCount() {
        assertEquals(3, ApplicationStatus.values().length);
    }

    @Test
    void applicationStatusValueOf() {
        assertEquals(ApplicationStatus.DRAFT, ApplicationStatus.valueOf("DRAFT"));
        assertEquals(ApplicationStatus.PENDING_ACTION, ApplicationStatus.valueOf("PENDING_ACTION"));
        assertEquals(ApplicationStatus.APPROVED, ApplicationStatus.valueOf("APPROVED"));
    }

    @Test
    void semesterValues() {
        assertEquals("I", Semester.I.getValue());
        assertEquals("II", Semester.II.getValue());
    }

    @Test
    void semesterValuesCount() {
        assertEquals(2, Semester.values().length);
    }

    @Test
    void semesterValueOf() {
        assertEquals(Semester.I, Semester.valueOf("I"));
        assertEquals(Semester.II, Semester.valueOf("II"));
    }
}
