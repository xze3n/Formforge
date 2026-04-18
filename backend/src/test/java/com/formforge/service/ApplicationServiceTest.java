package com.formforge.service;

import com.formforge.dto.CreateApplicationRequest;
import com.formforge.dto.PageResponse;
import com.formforge.dto.UpdateApplicationRequest;
import com.formforge.exception.ApplicationNotFoundException;
import com.formforge.model.Application;
import com.formforge.model.ApplicationStatus;
import com.formforge.model.ApplicationType;
import com.formforge.model.Semester;
import com.formforge.repository.InMemoryApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApplicationServiceTest {

    private InMemoryApplicationRepository repository;
    private ApplicationService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryApplicationRepository();
        repository.clear();
        service = new ApplicationService(repository);
    }

    // ── getAll (pagination) ────────────────────────────────────────────

    @Test
    void getAll_emptyRepository() {
        PageResponse<Application> response = service.getAll(0, 5);

        assertEquals(0, response.getTotalElements());
        assertEquals(0, response.getTotalPages());
        assertTrue(response.getContent().isEmpty());
    }

    @Test
    void getAll_firstPage() {
        seedApplications(8);

        PageResponse<Application> response = service.getAll(0, 5);

        assertEquals(5, response.getContent().size());
        assertEquals(0, response.getPage());
        assertEquals(5, response.getSize());
        assertEquals(8, response.getTotalElements());
        assertEquals(2, response.getTotalPages());
    }

    @Test
    void getAll_lastPage() {
        seedApplications(8);

        PageResponse<Application> response = service.getAll(1, 5);

        assertEquals(3, response.getContent().size());
        assertEquals(1, response.getPage());
    }

    @Test
    void getAll_pageOutOfRange() {
        seedApplications(3);

        PageResponse<Application> response = service.getAll(5, 5);

        assertTrue(response.getContent().isEmpty());
        assertEquals(3, response.getTotalElements());
    }

    @Test
    void getAll_singleItemPerPage() {
        seedApplications(3);

        PageResponse<Application> response = service.getAll(0, 1);

        assertEquals(1, response.getContent().size());
        assertEquals(3, response.getTotalPages());
    }

    @Test
    void getAll_exactlyOnePage() {
        seedApplications(5);

        PageResponse<Application> response = service.getAll(0, 5);

        assertEquals(5, response.getContent().size());
        assertEquals(1, response.getTotalPages());
    }

    // ── getById ────────────────────────────────────────────────────────

    @Test
    void getById_exists() {
        Application created = service.create(new CreateApplicationRequest(ApplicationType.MERIT, "2025/2026", Semester.I, null));

        Application found = service.getById(created.getId());

        assertEquals(created.getId(), found.getId());
        assertEquals(ApplicationType.MERIT, found.getType());
    }

    @Test
    void getById_notFound() {
        assertThrows(ApplicationNotFoundException.class, () -> service.getById(999L));
    }

    // ── create ─────────────────────────────────────────────────────────

    @Test
    void create_withDefaults() {
        CreateApplicationRequest request = new CreateApplicationRequest(ApplicationType.SOCIAL, "2024/2025", Semester.II, null);

        Application created = service.create(request);

        assertNotNull(created.getId());
        assertEquals(ApplicationType.SOCIAL, created.getType());
        assertEquals("2024/2025", created.getAcademicYear());
        assertEquals(Semester.II, created.getSemester());
        assertEquals(ApplicationStatus.DRAFT, created.getStatus());
        assertNotNull(created.getCreatedAt());
    }

    @Test
    void create_withExplicitStatus() {
        CreateApplicationRequest request = new CreateApplicationRequest(ApplicationType.PERFORMANCE, "2023/2024", Semester.I, ApplicationStatus.APPROVED);

        Application created = service.create(request);

        assertEquals(ApplicationStatus.APPROVED, created.getStatus());
    }

    // ── update ─────────────────────────────────────────────────────────

    @Test
    void update_allFields() {
        Application created = service.create(new CreateApplicationRequest(ApplicationType.MERIT, "2025/2026", Semester.I, null));

        UpdateApplicationRequest request = new UpdateApplicationRequest(ApplicationType.SOCIAL, "2024/2025", Semester.II, ApplicationStatus.APPROVED);
        Application updated = service.update(created.getId(), request);

        assertEquals(ApplicationType.SOCIAL, updated.getType());
        assertEquals("2024/2025", updated.getAcademicYear());
        assertEquals(Semester.II, updated.getSemester());
        assertEquals(ApplicationStatus.APPROVED, updated.getStatus());
    }

    @Test
    void update_partialFields() {
        Application created = service.create(new CreateApplicationRequest(ApplicationType.MERIT, "2025/2026", Semester.I, null));

        UpdateApplicationRequest request = new UpdateApplicationRequest();
        request.setAcademicYear("2024/2025");
        Application updated = service.update(created.getId(), request);

        assertEquals(ApplicationType.MERIT, updated.getType());
        assertEquals("2024/2025", updated.getAcademicYear());
        assertEquals(Semester.I, updated.getSemester());
    }

    @Test
    void update_noFields() {
        Application created = service.create(new CreateApplicationRequest(ApplicationType.MERIT, "2025/2026", Semester.I, null));

        UpdateApplicationRequest request = new UpdateApplicationRequest();
        Application updated = service.update(created.getId(), request);

        assertEquals(ApplicationType.MERIT, updated.getType());
        assertEquals("2025/2026", updated.getAcademicYear());
    }

    @Test
    void update_notFound() {
        UpdateApplicationRequest request = new UpdateApplicationRequest();
        assertThrows(ApplicationNotFoundException.class, () -> service.update(999L, request));
    }

    // ── delete ─────────────────────────────────────────────────────────

    @Test
    void delete_exists() {
        Application created = service.create(new CreateApplicationRequest(ApplicationType.MERIT, "2025/2026", Semester.I, null));

        assertDoesNotThrow(() -> service.delete(created.getId()));
        assertThrows(ApplicationNotFoundException.class, () -> service.getById(created.getId()));
    }

    @Test
    void delete_notFound() {
        assertThrows(ApplicationNotFoundException.class, () -> service.delete(999L));
    }

    // ── helpers ────────────────────────────────────────────────────────

    private void seedApplications(int count) {
        for (int i = 0; i < count; i++) {
            service.create(new CreateApplicationRequest(ApplicationType.MERIT, "2025/2026", Semester.I, null));
        }
    }
}
