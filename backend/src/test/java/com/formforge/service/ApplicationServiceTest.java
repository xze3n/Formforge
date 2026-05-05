package com.formforge.service;

import com.formforge.dto.CreateApplicationRequest;
import com.formforge.dto.PageResponse;
import com.formforge.dto.UpdateApplicationRequest;
import com.formforge.exception.ApplicationNotFoundException;
import com.formforge.model.Application;
import com.formforge.model.ApplicationStatus;
import com.formforge.model.ApplicationType;
import com.formforge.model.Semester;
import com.formforge.repository.ApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock
    private ApplicationRepository repository;

    private ApplicationService service;

    @BeforeEach
    void setUp() {
        service = new ApplicationService(repository);
    }

    // Ã¢â€â‚¬Ã¢â€â‚¬ getAll (pagination) Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬

    @Test
    void getAll_emptyRepository() {
        Page<Application> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 5), 0);
        when(repository.findAll(PageRequest.of(0, 5))).thenReturn(emptyPage);

        PageResponse<Application> response = service.getAll(0, 5);

        assertEquals(0, response.getTotalElements());
        assertEquals(0, response.getTotalPages());
        assertTrue(response.getContent().isEmpty());
    }

    @Test
    void getAll_firstPage() {
        List<Application> apps = buildApplications(5);
        Page<Application> page = new PageImpl<>(apps, PageRequest.of(0, 5), 8);
        when(repository.findAll(PageRequest.of(0, 5))).thenReturn(page);

        PageResponse<Application> response = service.getAll(0, 5);

        assertEquals(5, response.getContent().size());
        assertEquals(0, response.getPage());
        assertEquals(5, response.getSize());
        assertEquals(8, response.getTotalElements());
        assertEquals(2, response.getTotalPages());
    }

    @Test
    void getAll_lastPage() {
        List<Application> apps = buildApplications(3);
        Page<Application> page = new PageImpl<>(apps, PageRequest.of(1, 5), 8);
        when(repository.findAll(PageRequest.of(1, 5))).thenReturn(page);

        PageResponse<Application> response = service.getAll(1, 5);

        assertEquals(3, response.getContent().size());
        assertEquals(1, response.getPage());
    }

    @Test
    void getAll_pageOutOfRange() {
        Page<Application> emptyPage = new PageImpl<>(List.of(), PageRequest.of(5, 5), 3);
        when(repository.findAll(PageRequest.of(5, 5))).thenReturn(emptyPage);

        PageResponse<Application> response = service.getAll(5, 5);

        assertTrue(response.getContent().isEmpty());
        assertEquals(3, response.getTotalElements());
    }

    @Test
    void getAll_singleItemPerPage() {
        List<Application> apps = buildApplications(1);
        Page<Application> page = new PageImpl<>(apps, PageRequest.of(0, 1), 3);
        when(repository.findAll(PageRequest.of(0, 1))).thenReturn(page);

        PageResponse<Application> response = service.getAll(0, 1);

        assertEquals(1, response.getContent().size());
        assertEquals(3, response.getTotalPages());
    }

    @Test
    void getAll_exactlyOnePage() {
        List<Application> apps = buildApplications(5);
        Page<Application> page = new PageImpl<>(apps, PageRequest.of(0, 5), 5);
        when(repository.findAll(PageRequest.of(0, 5))).thenReturn(page);

        PageResponse<Application> response = service.getAll(0, 5);

        assertEquals(5, response.getContent().size());
        assertEquals(1, response.getTotalPages());
    }

    // Ã¢â€â‚¬Ã¢â€â‚¬ getById Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬

    // ── getByStatus ────────────────────────────────────────────────────

    @Test
    void getByStatus_returnsFilteredPage() {
        Page<Application> page = new PageImpl<>(buildApplications(3), PageRequest.of(0, 5), 3);
        when(repository.findByStatus(ApplicationStatus.APPROVED, PageRequest.of(0, 5))).thenReturn(page);

        PageResponse<Application> response = service.getByStatus(ApplicationStatus.APPROVED, 0, 5);

        assertEquals(3, response.getTotalElements());
        assertEquals(3, response.getContent().size());
        verify(repository).findByStatus(ApplicationStatus.APPROVED, PageRequest.of(0, 5));
    }

    @Test
    void getByStatus_empty() {
        Page<Application> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 5), 0);
        when(repository.findByStatus(ApplicationStatus.DRAFT, PageRequest.of(0, 5))).thenReturn(emptyPage);

        PageResponse<Application> response = service.getByStatus(ApplicationStatus.DRAFT, 0, 5);

        assertTrue(response.getContent().isEmpty());
        assertEquals(0, response.getTotalElements());
    }

    @Test
    void getByStatus_paginatesResults() {
        Page<Application> page0 = new PageImpl<>(buildApplications(5), PageRequest.of(0, 5), 8);
        Page<Application> page1 = new PageImpl<>(buildApplications(3), PageRequest.of(1, 5), 8);
        when(repository.findByStatus(ApplicationStatus.APPROVED, PageRequest.of(0, 5))).thenReturn(page0);
        when(repository.findByStatus(ApplicationStatus.APPROVED, PageRequest.of(1, 5))).thenReturn(page1);

        PageResponse<Application> response0 = service.getByStatus(ApplicationStatus.APPROVED, 0, 5);
        PageResponse<Application> response1 = service.getByStatus(ApplicationStatus.APPROVED, 1, 5);

        assertEquals(5, response0.getContent().size());
        assertEquals(3, response1.getContent().size());
        assertEquals(8, response0.getTotalElements());
        assertEquals(2, response0.getTotalPages());
    }

    // ── getByType ──────────────────────────────────────────────────────

    @Test
    void getByType_returnsFilteredPage() {
        Page<Application> page = new PageImpl<>(buildApplications(2), PageRequest.of(0, 5), 2);
        when(repository.findByType(ApplicationType.MERIT, PageRequest.of(0, 5))).thenReturn(page);

        PageResponse<Application> response = service.getByType(ApplicationType.MERIT, 0, 5);

        assertEquals(2, response.getTotalElements());
        assertEquals(2, response.getContent().size());
        verify(repository).findByType(ApplicationType.MERIT, PageRequest.of(0, 5));
    }

    @Test
    void getByType_empty() {
        Page<Application> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 5), 0);
        when(repository.findByType(ApplicationType.PERFORMANCE, PageRequest.of(0, 5))).thenReturn(emptyPage);

        PageResponse<Application> response = service.getByType(ApplicationType.PERFORMANCE, 0, 5);

        assertTrue(response.getContent().isEmpty());
        assertEquals(0, response.getTotalElements());
    }

    @Test
    void getByType_paginatesResults() {
        Page<Application> page0 = new PageImpl<>(buildApplications(3), PageRequest.of(0, 3), 7);
        Page<Application> page1 = new PageImpl<>(buildApplications(3), PageRequest.of(1, 3), 7);
        Page<Application> page2 = new PageImpl<>(buildApplications(1), PageRequest.of(2, 3), 7);
        when(repository.findByType(ApplicationType.SOCIAL, PageRequest.of(0, 3))).thenReturn(page0);
        when(repository.findByType(ApplicationType.SOCIAL, PageRequest.of(1, 3))).thenReturn(page1);
        when(repository.findByType(ApplicationType.SOCIAL, PageRequest.of(2, 3))).thenReturn(page2);

        PageResponse<Application> response0 = service.getByType(ApplicationType.SOCIAL, 0, 3);
        PageResponse<Application> response1 = service.getByType(ApplicationType.SOCIAL, 1, 3);
        PageResponse<Application> response2 = service.getByType(ApplicationType.SOCIAL, 2, 3);

        assertEquals(3, response0.getContent().size());
        assertEquals(3, response1.getContent().size());
        assertEquals(1, response2.getContent().size());
        assertEquals(7, response0.getTotalElements());
        assertEquals(3, response0.getTotalPages());
    }

    // ── getById ────────────────────────────────────────────────────────

    @Test
    void getById_exists() {
        Application app = buildApplication(1L, ApplicationType.MERIT, "2025/2026", Semester.I);
        when(repository.findById(1L)).thenReturn(Optional.of(app));

        Application found = service.getById(1L);

        assertEquals(1L, found.getId());
        assertEquals(ApplicationType.MERIT, found.getType());
    }

    @Test
    void getById_notFound() {
        when(repository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(ApplicationNotFoundException.class, () -> service.getById(999L));
    }

    // Ã¢â€â‚¬Ã¢â€â‚¬ create Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬

    @Test
    void create_withDefaults() {
        Application savedApp = buildApplication(1L, ApplicationType.SOCIAL, "2024/2025", Semester.II);
        savedApp.setStatus(ApplicationStatus.DRAFT);
        savedApp.setCreatedAt("4/29/2026");
        when(repository.save(any())).thenReturn(savedApp);

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
        Application savedApp = buildApplication(1L, ApplicationType.PERFORMANCE, "2023/2024", Semester.I);
        savedApp.setStatus(ApplicationStatus.APPROVED);
        when(repository.save(any())).thenReturn(savedApp);

        CreateApplicationRequest request = new CreateApplicationRequest(ApplicationType.PERFORMANCE, "2023/2024", Semester.I, ApplicationStatus.APPROVED);
        Application created = service.create(request);

        assertEquals(ApplicationStatus.APPROVED, created.getStatus());
    }

    // Ã¢â€â‚¬Ã¢â€â‚¬ update Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬

    @Test
    void update_allFields() {
        Application existing = buildApplication(1L, ApplicationType.MERIT, "2025/2026", Semester.I);
        existing.setStatus(ApplicationStatus.DRAFT);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateApplicationRequest request = new UpdateApplicationRequest(ApplicationType.SOCIAL, "2024/2025", Semester.II, ApplicationStatus.APPROVED);
        Application updated = service.update(1L, request);

        assertEquals(ApplicationType.SOCIAL, updated.getType());
        assertEquals("2024/2025", updated.getAcademicYear());
        assertEquals(Semester.II, updated.getSemester());
        assertEquals(ApplicationStatus.APPROVED, updated.getStatus());
    }

    @Test
    void update_partialFields() {
        Application existing = buildApplication(1L, ApplicationType.MERIT, "2025/2026", Semester.I);
        existing.setStatus(ApplicationStatus.DRAFT);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateApplicationRequest request = new UpdateApplicationRequest();
        request.setAcademicYear("2024/2025");
        Application updated = service.update(1L, request);

        assertEquals(ApplicationType.MERIT, updated.getType());
        assertEquals("2024/2025", updated.getAcademicYear());
        assertEquals(Semester.I, updated.getSemester());
    }

    @Test
    void update_noFields() {
        Application existing = buildApplication(1L, ApplicationType.MERIT, "2025/2026", Semester.I);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateApplicationRequest request = new UpdateApplicationRequest();
        Application updated = service.update(1L, request);

        assertEquals(ApplicationType.MERIT, updated.getType());
        assertEquals("2025/2026", updated.getAcademicYear());
    }

    @Test
    void update_notFound() {
        when(repository.findById(999L)).thenReturn(Optional.empty());
        UpdateApplicationRequest request = new UpdateApplicationRequest();
        assertThrows(ApplicationNotFoundException.class, () -> service.update(999L, request));
    }

    // Ã¢â€â‚¬Ã¢â€â‚¬ delete Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬

    @Test
    void delete_exists() {
        when(repository.existsById(1L)).thenReturn(true);
        assertDoesNotThrow(() -> service.delete(1L));
        verify(repository).deleteById(1L);
    }

    @Test
    void delete_notFound() {
        when(repository.existsById(999L)).thenReturn(false);
        assertThrows(ApplicationNotFoundException.class, () -> service.delete(999L));
    }

    // Ã¢â€â‚¬Ã¢â€â‚¬ getStatistics Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬

    @Test
    void getStatistics_returnsExpectedKeys() {
        List<Object[]> statusRows = new ArrayList<>();
        statusRows.add(new Object[]{ApplicationStatus.DRAFT, 2L});
        statusRows.add(new Object[]{ApplicationStatus.APPROVED, 1L});
        List<Object[]> typeRows = new ArrayList<>();
        typeRows.add(new Object[]{ApplicationType.MERIT, 3L});
        List<Object[]> semesterRows = new ArrayList<>();
        semesterRows.add(new Object[]{Semester.I, 2L});
        semesterRows.add(new Object[]{Semester.II, 1L});
        when(repository.countGroupByStatus()).thenReturn(statusRows);
        when(repository.countGroupByType()).thenReturn(typeRows);
        when(repository.countGroupBySemester()).thenReturn(semesterRows);

        var stats = service.getStatistics();

        assertTrue(stats.containsKey("byStatus"));
        assertTrue(stats.containsKey("byType"));
        assertTrue(stats.containsKey("bySemester"));
        assertEquals(2, stats.get("byStatus").size());
        assertEquals(1, stats.get("byType").size());
        assertEquals(2, stats.get("bySemester").size());
    }

    // Ã¢â€â‚¬Ã¢â€â‚¬ helpers Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬

    private Application buildApplication(Long id, ApplicationType type, String academicYear, Semester semester) {
        Application app = new Application();
        app.setId(id);
        app.setType(type);
        app.setAcademicYear(academicYear);
        app.setSemester(semester);
        return app;
    }

    private List<Application> buildApplications(int count) {
        return java.util.stream.IntStream.rangeClosed(1, count)
                .mapToObj(i -> buildApplication((long) i, ApplicationType.MERIT, "2025/2026", Semester.I))
                .toList();
    }
}
