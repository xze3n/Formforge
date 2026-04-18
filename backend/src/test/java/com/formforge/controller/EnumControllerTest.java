package com.formforge.controller;

import com.formforge.model.ApplicationStatus;
import com.formforge.model.ApplicationType;
import com.formforge.model.Semester;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Year;
import java.util.Arrays;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class EnumControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getAll_returnsOk() throws Exception {
        mockMvc.perform(get("/api/enums"))
                .andExpect(status().isOk());
    }

    @Test
    void getAll_containsAllKeys() throws Exception {
        mockMvc.perform(get("/api/enums"))
                .andExpect(jsonPath("$.types").isArray())
                .andExpect(jsonPath("$.statuses").isArray())
                .andExpect(jsonPath("$.semesters").isArray())
                .andExpect(jsonPath("$.academicYears").isArray());
    }

    @Test
    void getAll_typesMatchEnum() throws Exception {
        String[] expected = Arrays.stream(ApplicationType.values())
                .map(ApplicationType::getValue)
                .toArray(String[]::new);

        mockMvc.perform(get("/api/enums"))
                .andExpect(jsonPath("$.types", hasSize(expected.length)))
                .andExpect(jsonPath("$.types", containsInAnyOrder(expected)));
    }

    @Test
    void getAll_statusesMatchEnum() throws Exception {
        String[] expected = Arrays.stream(ApplicationStatus.values())
                .map(ApplicationStatus::getValue)
                .toArray(String[]::new);

        mockMvc.perform(get("/api/enums"))
                .andExpect(jsonPath("$.statuses", hasSize(expected.length)))
                .andExpect(jsonPath("$.statuses", containsInAnyOrder(expected)));
    }

    @Test
    void getAll_semestersMatchEnum() throws Exception {
        String[] expected = Arrays.stream(Semester.values())
                .map(Semester::getValue)
                .toArray(String[]::new);

        mockMvc.perform(get("/api/enums"))
                .andExpect(jsonPath("$.semesters", hasSize(expected.length)))
                .andExpect(jsonPath("$.semesters", containsInAnyOrder(expected)));
    }

    @Test
    void getAll_academicYearsBasedOnCurrentYear() throws Exception {
        int currentYear = Year.now().getValue();
        int expectedCount = 5; // currentYear-3 to currentYear+1

        mockMvc.perform(get("/api/enums"))
                .andExpect(jsonPath("$.academicYears", hasSize(expectedCount)))
                .andExpect(jsonPath("$.academicYears[0]", is((currentYear - 3) + "/" + (currentYear - 2))))
                .andExpect(jsonPath("$.academicYears[4]", is((currentYear + 1) + "/" + (currentYear + 2))));
    }

    @Test
    void getAll_academicYearsFormatIsCorrect() throws Exception {
        mockMvc.perform(get("/api/enums"))
                .andExpect(jsonPath("$.academicYears[*]", everyItem(matchesPattern("\\d{4}/\\d{4}"))));
    }
}
