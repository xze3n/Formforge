package com.formforge.dto;

import com.formforge.model.ApplicationStatus;
import com.formforge.model.ApplicationType;
import com.formforge.model.Semester;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DtoTest {

    @Test
    void createApplicationRequest_noArgs() {
        CreateApplicationRequest req = new CreateApplicationRequest();
        assertNull(req.getType());
        assertNull(req.getAcademicYear());
        assertNull(req.getSemester());
        assertNull(req.getStatus());
    }

    @Test
    void createApplicationRequest_allArgs() {
        CreateApplicationRequest req = new CreateApplicationRequest(
                ApplicationType.MERIT, "2025/2026", Semester.I, ApplicationStatus.DRAFT);

        assertEquals(ApplicationType.MERIT, req.getType());
        assertEquals("2025/2026", req.getAcademicYear());
        assertEquals(Semester.I, req.getSemester());
        assertEquals(ApplicationStatus.DRAFT, req.getStatus());
    }

    @Test
    void createApplicationRequest_setters() {
        CreateApplicationRequest req = new CreateApplicationRequest();
        req.setType(ApplicationType.SOCIAL);
        req.setAcademicYear("2024/2025");
        req.setSemester(Semester.II);
        req.setStatus(ApplicationStatus.APPROVED);

        assertEquals(ApplicationType.SOCIAL, req.getType());
        assertEquals("2024/2025", req.getAcademicYear());
        assertEquals(Semester.II, req.getSemester());
        assertEquals(ApplicationStatus.APPROVED, req.getStatus());
    }

    @Test
    void updateApplicationRequest_noArgs() {
        UpdateApplicationRequest req = new UpdateApplicationRequest();
        assertNull(req.getType());
        assertNull(req.getAcademicYear());
        assertNull(req.getSemester());
        assertNull(req.getStatus());
    }

    @Test
    void updateApplicationRequest_allArgs() {
        UpdateApplicationRequest req = new UpdateApplicationRequest(
                ApplicationType.PERFORMANCE, "2023/2024", Semester.II, ApplicationStatus.PENDING_ACTION);

        assertEquals(ApplicationType.PERFORMANCE, req.getType());
        assertEquals("2023/2024", req.getAcademicYear());
        assertEquals(Semester.II, req.getSemester());
        assertEquals(ApplicationStatus.PENDING_ACTION, req.getStatus());
    }

    @Test
    void updateApplicationRequest_setters() {
        UpdateApplicationRequest req = new UpdateApplicationRequest();
        req.setType(ApplicationType.MERIT);
        req.setAcademicYear("2025/2026");
        req.setSemester(Semester.I);
        req.setStatus(ApplicationStatus.DRAFT);

        assertEquals(ApplicationType.MERIT, req.getType());
        assertEquals("2025/2026", req.getAcademicYear());
        assertEquals(Semester.I, req.getSemester());
        assertEquals(ApplicationStatus.DRAFT, req.getStatus());
    }

    @Test
    void pageResponse_noArgs() {
        PageResponse<String> response = new PageResponse<>();
        assertNull(response.getContent());
        assertEquals(0, response.getPage());
        assertEquals(0, response.getSize());
        assertEquals(0, response.getTotalElements());
        assertEquals(0, response.getTotalPages());
    }

    @Test
    void pageResponse_allArgs() {
        List<String> content = List.of("a", "b", "c");
        PageResponse<String> response = new PageResponse<>(content, 0, 5, 3, 1);

        assertEquals(content, response.getContent());
        assertEquals(0, response.getPage());
        assertEquals(5, response.getSize());
        assertEquals(3, response.getTotalElements());
        assertEquals(1, response.getTotalPages());
    }

    @Test
    void pageResponse_setters() {
        PageResponse<String> response = new PageResponse<>();
        response.setContent(List.of("x"));
        response.setPage(2);
        response.setSize(10);
        response.setTotalElements(25);
        response.setTotalPages(3);

        assertEquals(List.of("x"), response.getContent());
        assertEquals(2, response.getPage());
        assertEquals(10, response.getSize());
        assertEquals(25, response.getTotalElements());
        assertEquals(3, response.getTotalPages());
    }
}
