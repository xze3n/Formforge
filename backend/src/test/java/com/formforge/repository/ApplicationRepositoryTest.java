package com.formforge.repository;

import com.formforge.model.Application;
import com.formforge.model.ApplicationStatus;
import com.formforge.model.ApplicationType;
import com.formforge.model.Semester;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@TestPropertySource(locations = "classpath:application.properties")
class ApplicationRepositoryTest {

    @Autowired
    private ApplicationRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    // ── findAll ────────────────────────────────────────────────────────

    @Test
    void findAll_empty() {
        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    void findAll_returnsAll() {
        repository.save(application(ApplicationType.MERIT, "2025/2026", Semester.I, null));
        repository.save(application(ApplicationType.SOCIAL, "2024/2025", Semester.II, null));
        assertEquals(2, repository.findAll().size());
    }

    // ── save / findById ────────────────────────────────────────────────

    @Test
    void save_assignsId() {
        Application saved = repository.save(application(ApplicationType.MERIT, "2025/2026", Semester.I, null));
        assertNotNull(saved.getId());
    }

    @Test
    void save_prePersistSetsDefaults() {
        Application app = new Application();
        app.setType(ApplicationType.MERIT);
        app.setAcademicYear("2025/2026");
        app.setSemester(Semester.I);
        // leave createdAt and status null → @PrePersist should fill them

        Application saved = repository.save(app);
        repository.flush(); // force SQL

        Application found = repository.findById(saved.getId()).orElseThrow();
        assertNotNull(found.getCreatedAt());
        assertEquals(ApplicationStatus.DRAFT, found.getStatus());
    }

    @Test
    void save_preservesExplicitStatus() {
        Application saved = repository.save(
                application(ApplicationType.SOCIAL, "2024/2025", Semester.II, ApplicationStatus.APPROVED));
        assertEquals(ApplicationStatus.APPROVED, saved.getStatus());
    }

    @Test
    void save_updatesExisting() {
        Application saved = repository.save(application(ApplicationType.MERIT, "2025/2026", Semester.I, null));
        saved.setType(ApplicationType.SOCIAL);
        repository.save(saved);

        Application found = repository.findById(saved.getId()).orElseThrow();
        assertEquals(ApplicationType.SOCIAL, found.getType());
        assertEquals(1, repository.count());
    }

    @Test
    void findById_exists() {
        Application saved = repository.save(application(ApplicationType.MERIT, "2025/2026", Semester.I, null));
        Optional<Application> found = repository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
    }

    @Test
    void findById_notFound() {
        assertTrue(repository.findById(999L).isEmpty());
    }

    // ── delete / existsById ────────────────────────────────────────────

    @Test
    void deleteById_removes() {
        Application saved = repository.save(application(ApplicationType.MERIT, "2025/2026", Semester.I, null));
        repository.deleteById(saved.getId());
        assertFalse(repository.existsById(saved.getId()));
    }

    @Test
    void existsById_true() {
        Application saved = repository.save(application(ApplicationType.MERIT, "2025/2026", Semester.I, null));
        assertTrue(repository.existsById(saved.getId()));
    }

    @Test
    void existsById_false() {
        assertFalse(repository.existsById(999L));
    }

    @Test
    void count_correct() {
        assertEquals(0, repository.count());
        repository.save(application(ApplicationType.MERIT, "2025/2026", Semester.I, null));
        repository.save(application(ApplicationType.SOCIAL, "2024/2025", Semester.II, null));
        assertEquals(2, repository.count());
    }

    // ── filter finders ─────────────────────────────────────────────────

    @Test
    void findByStatus_returnsMatchingOnly() {
        repository.save(application(ApplicationType.MERIT, "2025/2026", Semester.I, ApplicationStatus.APPROVED));
        repository.save(application(ApplicationType.SOCIAL, "2024/2025", Semester.II, ApplicationStatus.DRAFT));
        repository.save(application(ApplicationType.PERFORMANCE, "2023/2024", Semester.I, ApplicationStatus.APPROVED));

        List<Application> approved = repository.findByStatus(ApplicationStatus.APPROVED);

        assertEquals(2, approved.size());
        assertTrue(approved.stream().allMatch(a -> a.getStatus() == ApplicationStatus.APPROVED));
    }

    @Test
    void findByType_returnsMatchingOnly() {
        repository.save(application(ApplicationType.MERIT, "2025/2026", Semester.I, null));
        repository.save(application(ApplicationType.MERIT, "2024/2025", Semester.II, null));
        repository.save(application(ApplicationType.SOCIAL, "2023/2024", Semester.I, null));

        List<Application> merit = repository.findByType(ApplicationType.MERIT);

        assertEquals(2, merit.size());
        assertTrue(merit.stream().allMatch(a -> a.getType() == ApplicationType.MERIT));
    }

    @Test
    void findByAcademicYear_returnsMatchingOnly() {
        repository.save(application(ApplicationType.MERIT, "2025/2026", Semester.I, null));
        repository.save(application(ApplicationType.SOCIAL, "2025/2026", Semester.II, null));
        repository.save(application(ApplicationType.PERFORMANCE, "2024/2025", Semester.I, null));

        List<Application> results = repository.findByAcademicYear("2025/2026");

        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(a -> "2025/2026".equals(a.getAcademicYear())));
    }

    // ── statistics queries ─────────────────────────────────────────────

    @Test
    void countGroupByStatus_correctCounts() {
        repository.save(application(ApplicationType.MERIT, "2025/2026", Semester.I, ApplicationStatus.APPROVED));
        repository.save(application(ApplicationType.SOCIAL, "2024/2025", Semester.II, ApplicationStatus.APPROVED));
        repository.save(application(ApplicationType.PERFORMANCE, "2023/2024", Semester.I, ApplicationStatus.DRAFT));

        List<Object[]> rows = repository.countGroupByStatus();

        assertFalse(rows.isEmpty());
        long totalCounted = rows.stream().mapToLong(r -> (Long) r[1]).sum();
        assertEquals(3, totalCounted);
    }

    @Test
    void countGroupByType_correctCounts() {
        repository.save(application(ApplicationType.MERIT, "2025/2026", Semester.I, null));
        repository.save(application(ApplicationType.MERIT, "2024/2025", Semester.II, null));
        repository.save(application(ApplicationType.SOCIAL, "2023/2024", Semester.I, null));

        List<Object[]> rows = repository.countGroupByType();

        assertFalse(rows.isEmpty());
        long totalCounted = rows.stream().mapToLong(r -> (Long) r[1]).sum();
        assertEquals(3, totalCounted);
    }

    @Test
    void countGroupBySemester_correctCounts() {
        repository.save(application(ApplicationType.MERIT, "2025/2026", Semester.I, null));
        repository.save(application(ApplicationType.SOCIAL, "2024/2025", Semester.I, null));
        repository.save(application(ApplicationType.PERFORMANCE, "2023/2024", Semester.II, null));

        List<Object[]> rows = repository.countGroupBySemester();

        assertFalse(rows.isEmpty());
        long totalCounted = rows.stream().mapToLong(r -> (Long) r[1]).sum();
        assertEquals(3, totalCounted);
    }

    // ── helpers ────────────────────────────────────────────────────────

    private Application application(ApplicationType type, String academicYear, Semester semester, ApplicationStatus status) {
        Application app = new Application();
        app.setType(type);
        app.setAcademicYear(academicYear);
        app.setSemester(semester);
        app.setStatus(status);
        return app;
    }
}
