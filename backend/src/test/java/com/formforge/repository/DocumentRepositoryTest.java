package com.formforge.repository;

import com.formforge.model.Application;
import com.formforge.model.ApplicationType;
import com.formforge.model.Document;
import com.formforge.model.DocumentType;
import com.formforge.model.Semester;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@TestPropertySource(locations = "classpath:application.properties")
class DocumentRepositoryTest {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    private Long appId1;
    private Long appId2;

    @BeforeEach
    void setUp() {
        documentRepository.deleteAll();
        applicationRepository.deleteAll();

        Application a1 = new Application();
        a1.setType(ApplicationType.MERIT);
        a1.setAcademicYear("2025/2026");
        a1.setSemester(Semester.I);
        appId1 = applicationRepository.save(a1).getId();

        Application a2 = new Application();
        a2.setType(ApplicationType.SOCIAL);
        a2.setAcademicYear("2024/2025");
        a2.setSemester(Semester.II);
        appId2 = applicationRepository.save(a2).getId();
    }

    // ── findByApplicationId ────────────────────────────────────────────

    @Test
    void findByApplicationId_empty() {
        assertTrue(documentRepository.findByApplicationId(appId1).isEmpty());
    }

    @Test
    void findByApplicationId_returnsOnlyMatching() {
        documentRepository.save(doc(appId1, "Doc A", DocumentType.ID_COPY));
        documentRepository.save(doc(appId2, "Doc B", DocumentType.TAX_CERTIFICATE));
        documentRepository.save(doc(appId1, "Doc C", DocumentType.INCOME_CERTIFICATE));

        List<Document> docs = documentRepository.findByApplicationId(appId1);

        assertEquals(2, docs.size());
        assertTrue(docs.stream().allMatch(d -> d.getApplicationId().equals(appId1)));
    }

    // ── findById ──────────────────────────────────────────────────────

    @Test
    void findById_present() {
        Document saved = documentRepository.save(doc(appId1, "Doc", DocumentType.ID_COPY));
        Optional<Document> found = documentRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
    }

    @Test
    void findById_absent() {
        assertFalse(documentRepository.findById(999L).isPresent());
    }

    // ── save ──────────────────────────────────────────────────────────

    @Test
    void save_assignsId() {
        Document saved = documentRepository.save(doc(appId1, "Doc", DocumentType.ID_COPY));
        assertNotNull(saved.getId());
    }

    @Test
    void save_prePersistSetsDateAdded() {
        Document d = new Document();
        d.setApplicationId(appId1);
        d.setName("Doc");
        d.setType(DocumentType.ID_COPY);
        d.setVerified(false);
        Document saved = documentRepository.save(d);
        documentRepository.flush();

        Document found = documentRepository.findById(saved.getId()).orElseThrow();
        assertNotNull(found.getDateAdded());
        assertFalse(found.getDateAdded().isBlank());
    }

    @Test
    void save_preservesExplicitDateAdded() {
        Document d = doc(appId1, "Doc", DocumentType.ID_COPY);
        d.setDateAdded("1/1/2024");
        Document saved = documentRepository.save(d);
        assertEquals("1/1/2024", saved.getDateAdded());
    }

    @Test
    void save_updatesExisting() {
        Document saved = documentRepository.save(doc(appId1, "Original", DocumentType.ID_COPY));
        saved.setName("Updated");
        documentRepository.save(saved);

        Document found = documentRepository.findById(saved.getId()).orElseThrow();
        assertEquals("Updated", found.getName());
        assertEquals(1, documentRepository.findByApplicationId(appId1).size());
    }

    // ── deleteById ────────────────────────────────────────────────────

    @Test
    void deleteById_removes() {
        Document saved = documentRepository.save(doc(appId1, "Doc", DocumentType.ID_COPY));
        documentRepository.deleteById(saved.getId());
        assertFalse(documentRepository.existsById(saved.getId()));
    }

    // ── deleteByApplicationId ─────────────────────────────────────────

    @Test
    void deleteByApplicationId_removesAllForApp_leavesOthers() {
        documentRepository.save(doc(appId1, "A", DocumentType.ID_COPY));
        documentRepository.save(doc(appId1, "B", DocumentType.TAX_CERTIFICATE));
        documentRepository.save(doc(appId2, "C", DocumentType.INCOME_CERTIFICATE));

        documentRepository.deleteByApplicationId(appId1);

        assertTrue(documentRepository.findByApplicationId(appId1).isEmpty());
        assertEquals(1, documentRepository.findByApplicationId(appId2).size());
    }

    // ── existsById ────────────────────────────────────────────────────

    @Test
    void existsById_true() {
        Document saved = documentRepository.save(doc(appId1, "Doc", DocumentType.ID_COPY));
        assertTrue(documentRepository.existsById(saved.getId()));
    }

    @Test
    void existsById_false() {
        assertFalse(documentRepository.existsById(999L));
    }

    // ── findByType ────────────────────────────────────────────────────

    @Test
    void findByType_returnsMatchingOnly() {
        documentRepository.save(doc(appId1, "D1", DocumentType.ID_COPY));
        documentRepository.save(doc(appId1, "D2", DocumentType.ID_COPY));
        documentRepository.save(doc(appId2, "D3", DocumentType.TAX_CERTIFICATE));

        List<Document> idCopies = documentRepository.findByType(DocumentType.ID_COPY);

        assertEquals(2, idCopies.size());
        assertTrue(idCopies.stream().allMatch(d -> d.getType() == DocumentType.ID_COPY));
    }

    // ── findByVerified ────────────────────────────────────────────────

    @Test
    void findByVerified_returnsMatchingOnly() {
        Document d1 = doc(appId1, "Verified", DocumentType.ID_COPY);
        d1.setVerified(true);
        documentRepository.save(d1);
        documentRepository.save(doc(appId2, "NotVerified", DocumentType.TAX_CERTIFICATE));

        List<Document> verified = documentRepository.findByVerified(true);
        List<Document> notVerified = documentRepository.findByVerified(false);

        assertEquals(1, verified.size());
        assertEquals(1, notVerified.size());
    }

    // ── deleteByApplicationId ─────────────────────────────────────────

    @Test
    void deletingApplication_cascadesDocuments() {
        documentRepository.save(doc(appId1, "Doc A", DocumentType.ID_COPY));
        documentRepository.save(doc(appId1, "Doc B", DocumentType.TAX_CERTIFICATE));

        documentRepository.deleteByApplicationId(appId1);
        applicationRepository.deleteById(appId1);

        assertTrue(documentRepository.findByApplicationId(appId1).isEmpty());
    }

    // ── helpers ────────────────────────────────────────────────────────

    private Document doc(Long applicationId, String name, DocumentType type) {
        Document d = new Document();
        d.setApplicationId(applicationId);
        d.setName(name);
        d.setType(type);
        d.setVerified(false);
        return d;
    }
}
