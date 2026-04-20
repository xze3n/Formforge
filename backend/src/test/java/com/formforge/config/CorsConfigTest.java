package com.formforge.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CorsConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void corsAllowsLocalhostOrigin() throws Exception {
        mockMvc.perform(options("/api/applications")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    void corsAllowsGetMethod() throws Exception {
        mockMvc.perform(options("/api/applications")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Methods"));
    }

    @Test
    void corsAllowsDeleteMethod() throws Exception {
        mockMvc.perform(options("/api/applications")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "DELETE"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Methods"));
    }

    @Test
    void corsRejectsUnknownOrigin() throws Exception {
        mockMvc.perform(options("/api/applications")
                        .header("Origin", "http://evil.com")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
