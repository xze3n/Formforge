package com.formforge.service;

import com.formforge.model.Application;
import com.formforge.repository.InMemoryApplicationRepository;
import com.formforge.websocket.ApplicationWebSocketHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Year;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ApplicationGeneratorServiceTest {

    @Autowired
    private ApplicationGeneratorService generatorService;

    @Autowired
    private InMemoryApplicationRepository repository;

    @BeforeEach
    void setUp() {
        generatorService.stop();
        repository.clear();
    }

    @Test
    void start_returnsTrue_whenNotRunning() {
        assertTrue(generatorService.start());
        assertTrue(generatorService.isRunning());
    }

    @Test
    void start_returnsFalse_whenAlreadyRunning() {
        generatorService.start();
        assertFalse(generatorService.start());
    }

    @Test
    void stop_returnsTrue_whenRunning() {
        generatorService.start();
        assertTrue(generatorService.stop());
        assertFalse(generatorService.isRunning());
    }

    @Test
    void stop_returnsFalse_whenNotRunning() {
        assertFalse(generatorService.stop());
    }

    @Test
    void isRunning_initiallyFalse() {
        assertFalse(generatorService.isRunning());
    }

    @Test
    void generatesApplications_whenRunning() throws InterruptedException {
        generatorService.start();
        // Wait for at least one generation cycle (1 second interval, initial delay 0)
        Thread.sleep(1500);
        generatorService.stop();

        List<Application> apps = repository.findAll();
        assertFalse(apps.isEmpty(), "Generator should have created at least one application");

        Application app = apps.get(0);
        assertNotNull(app.getId());
        assertNotNull(app.getType());
        assertNotNull(app.getAcademicYear());
        assertNotNull(app.getSemester());
        assertNotNull(app.getStatus());
        assertNotNull(app.getCreatedAt());

        // Verify academic year format: YYYY/YYYY
        assertTrue(app.getAcademicYear().matches("\\d{4}/\\d{4}"));

        // Verify academic year is within expected range
        int currentYear = Year.now().getValue();
        String[] parts = app.getAcademicYear().split("/");
        int startYear = Integer.parseInt(parts[0]);
        assertTrue(startYear >= currentYear - 3 && startYear <= currentYear + 1);
        assertEquals(startYear + 1, Integer.parseInt(parts[1]));
    }

    @Test
    void stop_afterStart_preventsMoreGeneration() throws InterruptedException {
        generatorService.start();
        Thread.sleep(1500);
        generatorService.stop();

        long countAfterStop = repository.count();
        Thread.sleep(1500);
        assertEquals(countAfterStop, repository.count(),
                "No more applications should be generated after stop");
    }
}
