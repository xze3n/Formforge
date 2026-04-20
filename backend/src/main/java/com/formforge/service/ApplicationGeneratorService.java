package com.formforge.service;

import com.formforge.model.Application;
import com.formforge.model.ApplicationStatus;
import com.formforge.model.ApplicationType;
import com.formforge.model.Semester;
import com.formforge.repository.InMemoryApplicationRepository;
import com.formforge.websocket.ApplicationWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.stereotype.Service;

import java.time.Year;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationGeneratorService {

    private final InMemoryApplicationRepository repository;
    private final ApplicationWebSocketHandler webSocketHandler;
    private final Faker faker = new Faker();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ScheduledExecutorService scheduler;

    public boolean start() {
        if (!running.compareAndSet(false, true)) {
            return false; // already running
        }
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::generateOne, 0, 1, TimeUnit.SECONDS);
        log.info("Application generator started");
        return true;
    }

    public boolean stop() {
        if (!running.compareAndSet(true, false)) {
            return false; // not running
        }
        if (scheduler != null) {
            scheduler.shutdown();
            scheduler = null;
        }
        webSocketHandler.broadcastGeneratorStopped();
        log.info("Application generator stopped");
        return true;
    }

    public boolean isRunning() {
        return running.get();
    }

    private void generateOne() {
        try {
            int currentYear = Year.now().getValue();
            int startYear = faker.number().numberBetween(currentYear - 3, currentYear + 2);
            String academicYear = startYear + "/" + (startYear + 1);

            Application app = new Application();
            app.setType(faker.options().option(ApplicationType.class));
            app.setAcademicYear(academicYear);
            app.setSemester(faker.options().option(Semester.class));
            app.setStatus(faker.options().option(ApplicationStatus.class));

            Application saved = repository.save(app);
            webSocketHandler.broadcastNewApplication(saved);
            log.debug("Generated application #{}", saved.getId());
        } catch (Exception e) {
            log.error("Error generating application", e);
        }
    }
}
