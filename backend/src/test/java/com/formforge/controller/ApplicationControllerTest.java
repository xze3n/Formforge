package com.formforge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.formforge.dto.CreateApplicationRequest;
import com.formforge.dto.UpdateApplicationRequest;
import com.formforge.model.ApplicationStatus;
import com.formforge.model.ApplicationType;
import com.formforge.model.Semester;
import com.formforge.repository.InMemoryApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InMemoryApplicationRepository repository;

    @BeforeEach
    void setUp() {
        repository.clear();
    }

    // ── GET /api/applications ──────────────────────────────────────────

    @Test
    void getAll_empty() throws Exception {
        mockMvc.perform(get("/api/applications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements", is(0)))
                .andExpect(jsonPath("$.totalPages", is(0)));
    }

    @Test
    void getAll_defaultPagination() throws Exception {
        seedApplications(8);

        mockMvc.perform(get("/api/applications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(5)))
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.size", is(5)))
                .andExpect(jsonPath("$.totalElements", is(8)))
                .andExpect(jsonPath("$.totalPages", is(2)));
    }

    @Test
    void getAll_secondPage() throws Exception {
        seedApplications(8);

        mockMvc.perform(get("/api/applications?page=1&size=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.page", is(1)));
    }

    @Test
    void getAll_customPageSize() throws Exception {
        seedApplications(8);

        mockMvc.perform(get("/api/applications?page=0&size=3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.totalPages", is(3)));
    }

    @Test
    void getAll_pageOutOfRange() throws Exception {
        seedApplications(3);

        mockMvc.perform(get("/api/applications?page=10&size=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements", is(3)));
    }

    // ── GET /api/applications/{id} ─────────────────────────────────────

    @Test
    void getById_exists() throws Exception {
        CreateApplicationRequest request = new CreateApplicationRequest(ApplicationType.MERIT, "2025/2026", Semester.I, null);
        String response = mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();
        Long id = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/api/applications/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(id.intValue())))
                .andExpect(jsonPath("$.type", is("Merit")))
                .andExpect(jsonPath("$.academicYear", is("2025/2026")))
                .andExpect(jsonPath("$.semester", is("I")))
                .andExpect(jsonPath("$.status", is("Draft")));
    }

    @Test
    void getById_notFound() throws Exception {
        mockMvc.perform(get("/api/applications/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", containsString("999")));
    }

    // ── POST /api/applications ─────────────────────────────────────────

    @Test
    void create_valid() throws Exception {
        CreateApplicationRequest request = new CreateApplicationRequest(ApplicationType.SOCIAL, "2024/2025", Semester.II, null);

        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.type", is("Social")))
                .andExpect(jsonPath("$.academicYear", is("2024/2025")))
                .andExpect(jsonPath("$.semester", is("II")))
                .andExpect(jsonPath("$.status", is("Draft")))
                .andExpect(jsonPath("$.createdAt", notNullValue()));
    }

    @Test
    void create_withExplicitStatus() throws Exception {
        CreateApplicationRequest request = new CreateApplicationRequest(ApplicationType.PERFORMANCE, "2023/2024", Semester.I, ApplicationStatus.APPROVED);

        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("Approved")));
    }

    @Test
    void create_missingType() throws Exception {
        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"academicYear\":\"2025/2026\",\"semester\":\"I\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type", is("Type is required")));
    }

    @Test
    void create_missingAcademicYear() throws Exception {
        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"Merit\",\"semester\":\"I\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.academicYear", is("Academic year is required")));
    }

    @Test
    void create_missingSemester() throws Exception {
        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"Merit\",\"academicYear\":\"2025/2026\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.semester", is("Semester is required")));
    }

    @Test
    void create_invalidAcademicYearFormat() throws Exception {
        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"Merit\",\"academicYear\":\"2025\",\"semester\":\"I\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.academicYear", is("Academic year must be in format YYYY/YYYY")));
    }

    @Test
    void create_invalidType() throws Exception {
        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"Invalid\",\"academicYear\":\"2025/2026\",\"semester\":\"I\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_invalidSemester() throws Exception {
        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"Merit\",\"academicYear\":\"2025/2026\",\"semester\":\"III\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_emptyBody() throws Exception {
        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_allFieldsMissing() throws Exception {
        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type", is("Type is required")))
                .andExpect(jsonPath("$.academicYear", is("Academic year is required")))
                .andExpect(jsonPath("$.semester", is("Semester is required")));
    }

    // ── PUT /api/applications/{id} ─────────────────────────────────────

    @Test
    void update_allFields() throws Exception {
        Long id = createAndGetId(ApplicationType.MERIT, "2025/2026", Semester.I);

        UpdateApplicationRequest request = new UpdateApplicationRequest(ApplicationType.SOCIAL, "2024/2025", Semester.II, ApplicationStatus.APPROVED);

        mockMvc.perform(put("/api/applications/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type", is("Social")))
                .andExpect(jsonPath("$.academicYear", is("2024/2025")))
                .andExpect(jsonPath("$.semester", is("II")))
                .andExpect(jsonPath("$.status", is("Approved")));
    }

    @Test
    void update_partialFields() throws Exception {
        Long id = createAndGetId(ApplicationType.MERIT, "2025/2026", Semester.I);

        mockMvc.perform(put("/api/applications/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"academicYear\":\"2024/2025\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type", is("Merit")))
                .andExpect(jsonPath("$.academicYear", is("2024/2025")))
                .andExpect(jsonPath("$.semester", is("I")));
    }

    @Test
    void update_invalidAcademicYearFormat() throws Exception {
        Long id = createAndGetId(ApplicationType.MERIT, "2025/2026", Semester.I);

        mockMvc.perform(put("/api/applications/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"academicYear\":\"bad\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.academicYear", is("Academic year must be in format YYYY/YYYY")));
    }

    @Test
    void update_notFound() throws Exception {
        UpdateApplicationRequest request = new UpdateApplicationRequest();
        request.setAcademicYear("2024/2025");

        mockMvc.perform(put("/api/applications/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", containsString("999")));
    }

    // ── DELETE /api/applications/{id} ──────────────────────────────────

    @Test
    void delete_exists() throws Exception {
        Long id = createAndGetId(ApplicationType.MERIT, "2025/2026", Semester.I);

        mockMvc.perform(delete("/api/applications/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/applications/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_notFound() throws Exception {
        mockMvc.perform(delete("/api/applications/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", containsString("999")));
    }

    // ── Enum serialization ─────────────────────────────────────────────

    @Test
    void enumValues_serializedCorrectly() throws Exception {
        CreateApplicationRequest request = new CreateApplicationRequest(ApplicationType.PERFORMANCE, "2025/2026", Semester.I, ApplicationStatus.PENDING_ACTION);

        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type", is("Performance")))
                .andExpect(jsonPath("$.status", is("Pending Action")));
    }

    // ── helpers ────────────────────────────────────────────────────────

    private Long createAndGetId(ApplicationType type, String academicYear, Semester semester) throws Exception {
        CreateApplicationRequest request = new CreateApplicationRequest(type, academicYear, semester, null);
        String response = mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private void seedApplications(int count) throws Exception {
        for (int i = 0; i < count; i++) {
            CreateApplicationRequest request = new CreateApplicationRequest(ApplicationType.MERIT, "2025/2026", Semester.I, null);
            mockMvc.perform(post("/api/applications")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));
        }
    }
}
