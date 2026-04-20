package com.formforge.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApplicationTest {

    @Test
    void noArgsConstructor() {
        Application app = new Application();
        assertNull(app.getId());
        assertNull(app.getType());
        assertNull(app.getAcademicYear());
        assertNull(app.getSemester());
        assertNull(app.getCreatedAt());
        assertNull(app.getStatus());
    }

    @Test
    void allArgsConstructor() {
        Application app = new Application(1L, ApplicationType.MERIT, "2025/2026",
                Semester.I, "4/20/2026", ApplicationStatus.DRAFT);

        assertEquals(1L, app.getId());
        assertEquals(ApplicationType.MERIT, app.getType());
        assertEquals("2025/2026", app.getAcademicYear());
        assertEquals(Semester.I, app.getSemester());
        assertEquals("4/20/2026", app.getCreatedAt());
        assertEquals(ApplicationStatus.DRAFT, app.getStatus());
    }

    @Test
    void settersAndGetters() {
        Application app = new Application();
        app.setId(5L);
        app.setType(ApplicationType.SOCIAL);
        app.setAcademicYear("2024/2025");
        app.setSemester(Semester.II);
        app.setCreatedAt("1/15/2025");
        app.setStatus(ApplicationStatus.APPROVED);

        assertEquals(5L, app.getId());
        assertEquals(ApplicationType.SOCIAL, app.getType());
        assertEquals("2024/2025", app.getAcademicYear());
        assertEquals(Semester.II, app.getSemester());
        assertEquals("1/15/2025", app.getCreatedAt());
        assertEquals(ApplicationStatus.APPROVED, app.getStatus());
    }

    @Test
    void equalsAndHashCode() {
        Application app1 = new Application(1L, ApplicationType.MERIT, "2025/2026",
                Semester.I, "4/20/2026", ApplicationStatus.DRAFT);
        Application app2 = new Application(1L, ApplicationType.MERIT, "2025/2026",
                Semester.I, "4/20/2026", ApplicationStatus.DRAFT);

        assertEquals(app1, app2);
        assertEquals(app1.hashCode(), app2.hashCode());
    }

    @Test
    void notEquals() {
        Application app1 = new Application(1L, ApplicationType.MERIT, "2025/2026",
                Semester.I, "4/20/2026", ApplicationStatus.DRAFT);
        Application app2 = new Application(2L, ApplicationType.SOCIAL, "2024/2025",
                Semester.II, "1/1/2025", ApplicationStatus.APPROVED);

        assertNotEquals(app1, app2);
    }

    @Test
    void toStringContainsFields() {
        Application app = new Application(1L, ApplicationType.MERIT, "2025/2026",
                Semester.I, "4/20/2026", ApplicationStatus.DRAFT);
        String str = app.toString();

        assertTrue(str.contains("1"));
        assertTrue(str.contains("2025/2026"));
    }
}
