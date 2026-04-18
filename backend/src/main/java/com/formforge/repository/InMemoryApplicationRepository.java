package com.formforge.repository;

import com.formforge.model.Application;
import com.formforge.model.ApplicationStatus;
import com.formforge.model.ApplicationType;
import com.formforge.model.Semester;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryApplicationRepository {

    private final Map<Long, Application> store = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(0);

    public InMemoryApplicationRepository() {
        seedData();
    }

    public List<Application> findAll() {
        return new ArrayList<>(store.values());
    }

    public Optional<Application> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    public Application save(Application application) {
        if (application.getId() == null) {
            application.setId(idGenerator.incrementAndGet());
        }
        if (application.getCreatedAt() == null) {
            application.setCreatedAt(LocalDate.now().format(DateTimeFormatter.ofPattern("M/d/yyyy")));
        }
        if (application.getStatus() == null) {
            application.setStatus(ApplicationStatus.DRAFT);
        }
        store.put(application.getId(), application);
        return application;
    }

    public boolean deleteById(Long id) {
        return store.remove(id) != null;
    }

    public boolean existsById(Long id) {
        return store.containsKey(id);
    }

    public long count() {
        return store.size();
    }

    public void clear() {
        store.clear();
        idGenerator.set(0);
    }

    private void seedData() {
        save(new Application(null, ApplicationType.MERIT, "2025/2026", Semester.I, "4/1/2025", ApplicationStatus.DRAFT));
        save(new Application(null, ApplicationType.SOCIAL, "2025/2026", Semester.I, "3/15/2025", ApplicationStatus.PENDING_ACTION));
        save(new Application(null, ApplicationType.PERFORMANCE, "2024/2025", Semester.II, "1/20/2025", ApplicationStatus.APPROVED));
        save(new Application(null, ApplicationType.MERIT, "2024/2025", Semester.I, "9/10/2024", ApplicationStatus.APPROVED));
        save(new Application(null, ApplicationType.SOCIAL, "2024/2025", Semester.II, "2/5/2025", ApplicationStatus.DRAFT));
        save(new Application(null, ApplicationType.PERFORMANCE, "2023/2024", Semester.I, "10/1/2023", ApplicationStatus.APPROVED));
        save(new Application(null, ApplicationType.MERIT, "2023/2024", Semester.II, "3/20/2024", ApplicationStatus.PENDING_ACTION));
        save(new Application(null, ApplicationType.SOCIAL, "2025/2026", Semester.II, "4/10/2025", ApplicationStatus.DRAFT));
    }
}
