package com.formforge.controller;

import com.formforge.repository.InMemoryApplicationRepository;
import com.formforge.service.ApplicationGeneratorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class GeneratorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationGeneratorService generatorService;

    @Autowired
    private InMemoryApplicationRepository repository;

    @BeforeEach
    void setUp() {
        // Ensure generator is stopped before each test
        generatorService.stop();
        repository.clear();
    }

    // ── POST /api/generator/start ──────────────────────────────────────

    @Test
    void start_returnsRunningTrue() throws Exception {
        mockMvc.perform(post("/api/generator/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.running", is(true)))
                .andExpect(jsonPath("$.started", is(true)));
    }

    @Test
    void start_whenAlreadyRunning_returnsStartedFalse() throws Exception {
        generatorService.start();

        mockMvc.perform(post("/api/generator/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.running", is(true)))
                .andExpect(jsonPath("$.started", is(false)));
    }

    // ── POST /api/generator/stop ───────────────────────────────────────

    @Test
    void stop_whenRunning_returnsStoppedTrue() throws Exception {
        generatorService.start();

        mockMvc.perform(post("/api/generator/stop"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.running", is(false)))
                .andExpect(jsonPath("$.stopped", is(true)));
    }

    @Test
    void stop_whenNotRunning_returnsStoppedFalse() throws Exception {
        mockMvc.perform(post("/api/generator/stop"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.running", is(false)))
                .andExpect(jsonPath("$.stopped", is(false)));
    }

    // ── GET /api/generator/status ──────────────────────────────────────

    @Test
    void status_whenNotRunning_returnsFalse() throws Exception {
        mockMvc.perform(get("/api/generator/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.running", is(false)));
    }

    @Test
    void status_whenRunning_returnsTrue() throws Exception {
        generatorService.start();

        mockMvc.perform(get("/api/generator/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.running", is(true)));
    }
}
