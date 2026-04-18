package com.formforge.repository;

import com.formforge.model.Application;
import com.formforge.model.ApplicationStatus;
import com.formforge.model.ApplicationType;
import com.formforge.model.Semester;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryApplicationRepositoryTest {

    private InMemoryApplicationRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryApplicationRepository();
        repository.clear();
    }

    @Test
    void findAll_empty() {
        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    void save_assignsIdAndDefaults() {
        Application app = new Application();
        app.setType(ApplicationType.MERIT);
        app.setAcademicYear("2025/2026");
        app.setSemester(Semester.I);

        Application saved = repository.save(app);

        assertNotNull(saved.getId());
        assertEquals(1L, saved.getId());
        assertNotNull(saved.getCreatedAt());
        assertEquals(ApplicationStatus.DRAFT, saved.getStatus());
    }

    @Test
    void save_preservesExistingValues() {
        Application app = new Application(null, ApplicationType.SOCIAL, "2024/2025", Semester.II, "1/1/2025", ApplicationStatus.APPROVED);

        Application saved = repository.save(app);

        assertEquals("1/1/2025", saved.getCreatedAt());
        assertEquals(ApplicationStatus.APPROVED, saved.getStatus());
    }

    @Test
    void save_updatesExistingApplication() {
        Application app = new Application();
        app.setType(ApplicationType.MERIT);
        app.setAcademicYear("2025/2026");
        app.setSemester(Semester.I);
        Application saved = repository.save(app);

        saved.setType(ApplicationType.SOCIAL);
        repository.save(saved);

        Application found = repository.findById(saved.getId()).orElseThrow();
        assertEquals(ApplicationType.SOCIAL, found.getType());
        assertEquals(1, repository.count());
    }

    @Test
    void findById_exists() {
        Application app = new Application(null, ApplicationType.MERIT, "2025/2026", Semester.I, null, null);
        Application saved = repository.save(app);

        Optional<Application> found = repository.findById(saved.getId());

        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
    }

    @Test
    void findById_notFound() {
        Optional<Application> found = repository.findById(999L);
        assertTrue(found.isEmpty());
    }

    @Test
    void deleteById_exists() {
        Application app = new Application(null, ApplicationType.MERIT, "2025/2026", Semester.I, null, null);
        Application saved = repository.save(app);

        assertTrue(repository.deleteById(saved.getId()));
        assertEquals(0, repository.count());
    }

    @Test
    void deleteById_notFound() {
        assertFalse(repository.deleteById(999L));
    }

    @Test
    void existsById() {
        Application app = new Application(null, ApplicationType.MERIT, "2025/2026", Semester.I, null, null);
        Application saved = repository.save(app);

        assertTrue(repository.existsById(saved.getId()));
        assertFalse(repository.existsById(999L));
    }

    @Test
    void count() {
        assertEquals(0, repository.count());

        repository.save(new Application(null, ApplicationType.MERIT, "2025/2026", Semester.I, null, null));
        repository.save(new Application(null, ApplicationType.SOCIAL, "2024/2025", Semester.II, null, null));

        assertEquals(2, repository.count());
    }

    @Test
    void clear_removesAllAndResetsIds() {
        repository.save(new Application(null, ApplicationType.MERIT, "2025/2026", Semester.I, null, null));
        repository.save(new Application(null, ApplicationType.SOCIAL, "2024/2025", Semester.II, null, null));

        repository.clear();

        assertEquals(0, repository.count());
        Application next = repository.save(new Application(null, ApplicationType.PERFORMANCE, "2023/2024", Semester.I, null, null));
        assertEquals(1L, next.getId());
    }

    @Test
    void findAll_returnsDefensiveCopy() {
        repository.save(new Application(null, ApplicationType.MERIT, "2025/2026", Semester.I, null, null));

        List<Application> list1 = repository.findAll();
        List<Application> list2 = repository.findAll();

        assertNotSame(list1, list2);
    }

    @Test
    void seedData_populatesOnConstruction() {
        InMemoryApplicationRepository fresh = new InMemoryApplicationRepository();
        assertEquals(8, fresh.count());
        assertTrue(fresh.findById(1L).isPresent());
        assertTrue(fresh.findById(8L).isPresent());
    }

    @Test
    void save_autoIncrementsIds() {
        Application a1 = repository.save(new Application(null, ApplicationType.MERIT, "2025/2026", Semester.I, null, null));
        Application a2 = repository.save(new Application(null, ApplicationType.SOCIAL, "2024/2025", Semester.II, null, null));

        assertEquals(1L, a1.getId());
        assertEquals(2L, a2.getId());
    }
}
