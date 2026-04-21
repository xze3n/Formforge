package com.formforge.repository;

import com.formforge.model.Document;
import com.formforge.model.DocumentType;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class InMemoryDocumentRepository {

    private final Map<Long, Document> store = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(0);

    public InMemoryDocumentRepository() {
        seedData();
    }

    public List<Document> findByApplicationId(Long applicationId) {
        return store.values().stream()
                .filter(d -> applicationId.equals(d.getApplicationId()))
                .sorted(Comparator.comparingLong(Document::getId))
                .collect(Collectors.toList());
    }

    public Optional<Document> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    public Document save(Document document) {
        if (document.getId() == null) {
            document.setId(idGenerator.incrementAndGet());
        }
        if (document.getDateAdded() == null) {
            document.setDateAdded(LocalDate.now().format(DateTimeFormatter.ofPattern("M/d/yyyy")));
        }
        store.put(document.getId(), document);
        return document;
    }

    public boolean deleteById(Long id) {
        return store.remove(id) != null;
    }

    public void deleteByApplicationId(Long applicationId) {
        store.values().removeIf(d -> applicationId.equals(d.getApplicationId()));
    }

    public boolean existsById(Long id) {
        return store.containsKey(id);
    }

    public void clear() {
        store.clear();
        idGenerator.set(0);
    }

    private void seedData() {
        save(new Document(null, 1L, "Enrollment Certificate 2025", DocumentType.STUDENT_ENROLLMENT_CERTIFICATE,
                "Official enrollment certificate for academic year 2025/2026", "4/1/2025", true, null));
        save(new Document(null, 1L, "Income Certificate", DocumentType.INCOME_CERTIFICATE,
                "Family income certificate issued by employer", "4/2/2025", false, "Needs notarization"));
        save(new Document(null, 2L, "Social Assessment Report", DocumentType.SOCIAL_ASSESSMENT_REPORT,
                "Assessment conducted by social services department", "3/16/2025", true, null));
        save(new Document(null, 2L, "ID Copy", DocumentType.ID_COPY,
                "Certified copy of national ID", "3/17/2025", true, null));
        save(new Document(null, 3L, "Tax Certificate", DocumentType.TAX_CERTIFICATE,
                "Annual tax clearance certificate", "1/21/2025", false, "Pending verification from tax office"));
    }
}
