package com.formforge.service;

import com.formforge.dto.CreateDocumentRequest;
import com.formforge.dto.UpdateDocumentRequest;
import com.formforge.exception.DocumentNotFoundException;
import com.formforge.model.Document;
import com.formforge.model.DocumentType;
import com.formforge.repository.InMemoryDocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DocumentServiceTest {

    private InMemoryDocumentRepository repository;
    private DocumentService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryDocumentRepository();
        repository.clear();
        service = new DocumentService(repository);
    }

    // ── getByApplicationId ─────────────────────────────────────────────

    @Test
    void getByApplicationId_empty() {
        List<Document> docs = service.getByApplicationId(99L);
        assertTrue(docs.isEmpty());
    }

    @Test
    void getByApplicationId_returnsDocs() {
        repository.save(new Document(null, 1L, "Doc", DocumentType.ID_COPY, null, null, false, null));

        List<Document> docs = service.getByApplicationId(1L);

        assertEquals(1, docs.size());
    }

    // ── getById ────────────────────────────────────────────────────────

    @Test
    void getById_found() {
        Document saved = repository.save(new Document(null, 1L, "Doc", DocumentType.ID_COPY, null, null, false, null));

        Document found = service.getById(saved.getId());

        assertEquals(saved.getId(), found.getId());
    }

    @Test
    void getById_notFound_throwsDocumentNotFoundException() {
        DocumentNotFoundException ex = assertThrows(
                DocumentNotFoundException.class,
                () -> service.getById(999L)
        );
        assertTrue(ex.getMessage().contains("999"));
    }

    // ── create ─────────────────────────────────────────────────────────

    @Test
    void create_setsAllFields() {
        CreateDocumentRequest req = new CreateDocumentRequest();
        req.setName("Tax Doc");
        req.setType(DocumentType.TAX_CERTIFICATE);
        req.setDescription("Annual tax clearance");
        req.setNotes("Pending stamp");

        Document created = service.create(5L, req);

        assertNotNull(created.getId());
        assertEquals(5L,                        created.getApplicationId());
        assertEquals("Tax Doc",                  created.getName());
        assertEquals(DocumentType.TAX_CERTIFICATE, created.getType());
        assertEquals("Annual tax clearance",     created.getDescription());
        assertEquals("Pending stamp",            created.getNotes());
        assertFalse(created.isVerified());
        assertNotNull(created.getDateAdded());
    }

    @Test
    void create_verifiedAlwaysFalse() {
        CreateDocumentRequest req = new CreateDocumentRequest();
        req.setName("Doc");
        req.setType(DocumentType.ID_COPY);

        Document created = service.create(1L, req);

        assertFalse(created.isVerified());
    }

    // ── update ─────────────────────────────────────────────────────────

    @Test
    void update_allFieldsSet() {
        Document saved = repository.save(
                new Document(null, 1L, "Old Name", DocumentType.ID_COPY, "old desc", null, false, "old notes"));

        UpdateDocumentRequest req = new UpdateDocumentRequest();
        req.setName("New Name");
        req.setType(DocumentType.TAX_CERTIFICATE);
        req.setDescription("new desc");
        req.setNotes("new notes");
        req.setVerified(true);

        Document updated = service.update(saved.getId(), req);

        assertEquals("New Name",                  updated.getName());
        assertEquals(DocumentType.TAX_CERTIFICATE, updated.getType());
        assertEquals("new desc",                  updated.getDescription());
        assertEquals("new notes",                 updated.getNotes());
        assertTrue(updated.isVerified());
    }

    @Test
    void update_nullFieldsNotChanged() {
        Document saved = repository.save(
                new Document(null, 1L, "Name", DocumentType.ID_COPY, "desc", null, false, "notes"));

        UpdateDocumentRequest req = new UpdateDocumentRequest();
        // all fields null

        Document updated = service.update(saved.getId(), req);

        assertEquals("Name",             updated.getName());
        assertEquals(DocumentType.ID_COPY, updated.getType());
        assertEquals("desc",             updated.getDescription());
        assertEquals("notes",            updated.getNotes());
        assertFalse(updated.isVerified());
    }

    @Test
    void update_notFound_throwsDocumentNotFoundException() {
        UpdateDocumentRequest req = new UpdateDocumentRequest();
        assertThrows(DocumentNotFoundException.class, () -> service.update(999L, req));
    }

    // ── delete ─────────────────────────────────────────────────────────

    @Test
    void delete_success() {
        Document saved = repository.save(new Document(null, 1L, "Doc", DocumentType.ID_COPY, null, null, false, null));

        assertDoesNotThrow(() -> service.delete(saved.getId()));

        assertFalse(repository.findById(saved.getId()).isPresent());
    }

    @Test
    void delete_notFound_throwsDocumentNotFoundException() {
        assertThrows(DocumentNotFoundException.class, () -> service.delete(999L));
    }
}
