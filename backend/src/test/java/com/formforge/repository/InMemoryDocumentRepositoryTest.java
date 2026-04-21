package com.formforge.repository;

import com.formforge.model.Document;
import com.formforge.model.DocumentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryDocumentRepositoryTest {

    private InMemoryDocumentRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryDocumentRepository();
        repository.clear();
    }

    // ── findByApplicationId ────────────────────────────────────────────

    @Test
    void findByApplicationId_noDocuments() {
        List<Document> docs = repository.findByApplicationId(99L);
        assertTrue(docs.isEmpty());
    }

    @Test
    void findByApplicationId_returnsOnlyMatchingApp() {
        repository.save(new Document(null, 1L, "Doc A", DocumentType.ID_COPY, null, null, false, null));
        repository.save(new Document(null, 2L, "Doc B", DocumentType.TAX_CERTIFICATE, null, null, false, null));
        repository.save(new Document(null, 1L, "Doc C", DocumentType.INCOME_CERTIFICATE, null, null, false, null));

        List<Document> docs = repository.findByApplicationId(1L);

        assertEquals(2, docs.size());
        assertTrue(docs.stream().allMatch(d -> d.getApplicationId().equals(1L)));
    }

    @Test
    void findByApplicationId_sortedById() {
        Document d1 = repository.save(new Document(null, 5L, "First",  DocumentType.ID_COPY, null, null, false, null));
        Document d2 = repository.save(new Document(null, 5L, "Second", DocumentType.TAX_CERTIFICATE, null, null, false, null));

        List<Document> docs = repository.findByApplicationId(5L);

        assertEquals(d1.getId(), docs.get(0).getId());
        assertEquals(d2.getId(), docs.get(1).getId());
    }

    // ── findById ──────────────────────────────────────────────────────

    @Test
    void findById_present() {
        Document saved = repository.save(new Document(null, 1L, "Doc", DocumentType.ID_COPY, null, null, false, null));

        Optional<Document> found = repository.findById(saved.getId());

        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
    }

    @Test
    void findById_absent() {
        Optional<Document> found = repository.findById(999L);
        assertFalse(found.isPresent());
    }

    // ── save ──────────────────────────────────────────────────────────

    @Test
    void save_assignsIdWhenNull() {
        Document doc = new Document(null, 1L, "Doc", DocumentType.ID_COPY, "desc", null, false, null);

        Document saved = repository.save(doc);

        assertNotNull(saved.getId());
        assertEquals(1L, saved.getId());
    }

    @Test
    void save_assignsDateAddedWhenNull() {
        Document doc = new Document(null, 1L, "Doc", DocumentType.ID_COPY, null, null, false, null);

        Document saved = repository.save(doc);

        assertNotNull(saved.getDateAdded());
        assertFalse(saved.getDateAdded().isBlank());
    }

    @Test
    void save_preservesExistingDateAdded() {
        Document doc = new Document(null, 1L, "Doc", DocumentType.ID_COPY, null, "1/1/2024", false, null);

        Document saved = repository.save(doc);

        assertEquals("1/1/2024", saved.getDateAdded());
    }

    @Test
    void save_updatesExistingDocument() {
        Document saved = repository.save(new Document(null, 1L, "Original", DocumentType.ID_COPY, null, null, false, null));

        saved.setName("Updated");
        repository.save(saved);

        Document found = repository.findById(saved.getId()).orElseThrow();
        assertEquals("Updated", found.getName());
        assertEquals(1, repository.findByApplicationId(1L).size());
    }

    // ── deleteById ────────────────────────────────────────────────────

    @Test
    void deleteById_existingDocument_returnsTrue() {
        Document saved = repository.save(new Document(null, 1L, "Doc", DocumentType.ID_COPY, null, null, false, null));

        boolean result = repository.deleteById(saved.getId());

        assertTrue(result);
        assertFalse(repository.findById(saved.getId()).isPresent());
    }

    @Test
    void deleteById_nonExistentDocument_returnsFalse() {
        boolean result = repository.deleteById(999L);
        assertFalse(result);
    }

    // ── deleteByApplicationId ─────────────────────────────────────────

    @Test
    void deleteByApplicationId_removesAllForApp_leavesOthers() {
        repository.save(new Document(null, 1L, "A", DocumentType.ID_COPY, null, null, false, null));
        repository.save(new Document(null, 1L, "B", DocumentType.TAX_CERTIFICATE, null, null, false, null));
        repository.save(new Document(null, 2L, "C", DocumentType.INCOME_CERTIFICATE, null, null, false, null));

        repository.deleteByApplicationId(1L);

        assertTrue(repository.findByApplicationId(1L).isEmpty());
        assertEquals(1, repository.findByApplicationId(2L).size());
    }

    // ── existsById ────────────────────────────────────────────────────

    @Test
    void existsById_true() {
        Document saved = repository.save(new Document(null, 1L, "Doc", DocumentType.ID_COPY, null, null, false, null));
        assertTrue(repository.existsById(saved.getId()));
    }

    @Test
    void existsById_false() {
        assertFalse(repository.existsById(999L));
    }

    // ── clear ─────────────────────────────────────────────────────────

    @Test
    void clear_removesAllDocumentsAndResetsIdGenerator() {
        repository.save(new Document(null, 1L, "Doc", DocumentType.ID_COPY, null, null, false, null));

        repository.clear();

        assertTrue(repository.findByApplicationId(1L).isEmpty());

        // id generator resets to 1 after clear
        Document after = repository.save(new Document(null, 1L, "New", DocumentType.ID_COPY, null, null, false, null));
        assertEquals(1L, after.getId());
    }
}
