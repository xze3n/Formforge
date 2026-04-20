package com.formforge.exception;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void notFound_returns404WithMessage() throws Exception {
        mockMvc.perform(get("/api/applications/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("Application not found with id: 99999")));
    }

    @Test
    void validationError_returns400WithFieldErrors() throws Exception {
        // Missing required fields
        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type", is("Type is required")))
                .andExpect(jsonPath("$.academicYear", is("Academic year is required")))
                .andExpect(jsonPath("$.semester", is("Semester is required")));
    }

    @Test
    void invalidRequestBody_returns400() throws Exception {
        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("not valid json at all"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("Invalid request body")));
    }

    @Test
    void applicationNotFoundException_hasCorrectMessage() {
        ApplicationNotFoundException ex = new ApplicationNotFoundException(42L);
        assert ex.getMessage().equals("Application not found with id: 42");
    }
}
