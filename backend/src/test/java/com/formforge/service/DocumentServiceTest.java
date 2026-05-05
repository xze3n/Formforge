package com.formforge.service;

import com.formforge.dto.CreateDocumentRequest;
import com.formforge.dto.UpdateDocumentRequest;
import com.formforge.exception.DocumentNotFoundException;
import com.formforge.model.Document;
import com.formforge.model.DocumentType;
import com.formforge.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository repository;

    private DocumentService service;

    @BeforeEach
    void setUp() {
        service = new DocumentService(repository);
    }

    // â”€â”€ getByApplicationId â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    void getByApplicationId_empty() {
        when(repository.findByApplicationId(99L)).thenReturn(List.of());
        List<Document> docs = service.getByApplicationId(99L);
        assertTrue(docs.isEmpty());
    }

    @Test
    void getByApplicationId_returnsDocs() {
        Document doc = buildDoc(1L, 1L, "Doc", DocumentType.ID_COPY);
        when(repository.findByApplicationId(1L)).thenReturn(List.of(doc));

        List<Document> docs = service.getByApplicationId(1L);

        assertEquals(1, docs.size());
    }

    // â”€â”€ getById â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    void getById_found() {
        Document doc = buildDoc(1L, 1L, "Doc", DocumentType.ID_COPY);
        when(repository.findById(1L)).thenReturn(Optional.of(doc));

        Document found = service.getById(1L);

        assertEquals(1L, found.getId());
    }

    @Test
    void getById_notFound_throwsDocumentNotFoundException() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        DocumentNotFoundException ex = assertThrows(
                DocumentNotFoundException.class,
                () -> service.getById(999L)
        );
        assertTrue(ex.getMessage().contains("999"));
    }

    // â”€â”€ create â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    void create_setsAllFields() {
        Document savedDoc = buildDoc(1L, 5L, "Tax Doc", DocumentType.TAX_CERTIFICATE);
        savedDoc.setDescription("Annual tax clearance");
        savedDoc.setNotes("Pending stamp");
        savedDoc.setDateAdded("4/29/2026");
        when(repository.save(any())).thenReturn(savedDoc);

        CreateDocumentRequest req = new CreateDocumentRequest();
        req.setName("Tax Doc");
        req.setType(DocumentType.TAX_CERTIFICATE);
        req.setDescription("Annual tax clearance");
        req.setNotes("Pending stamp");

        Document created = service.create(5L, req);

        assertNotNull(created.getId());
        assertEquals(5L, created.getApplicationId());
        assertEquals("Tax Doc", created.getName());
        assertEquals(DocumentType.TAX_CERTIFICATE, created.getType());
        assertEquals("Annual tax clearance", created.getDescription());
        assertEquals("Pending stamp", created.getNotes());
        assertFalse(created.isVerified());
        assertNotNull(created.getDateAdded());
    }

    @Test
    void create_verifiedAlwaysFalse() {
        Document savedDoc = buildDoc(1L, 1L, "Doc", DocumentType.ID_COPY);
        when(repository.save(any())).thenReturn(savedDoc);

        CreateDocumentRequest req = new CreateDocumentRequest();
        req.setName("Doc");
        req.setType(DocumentType.ID_COPY);

        Document created = service.create(1L, req);

        assertFalse(created.isVerified());
    }

    // â”€â”€ update â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    void update_allFieldsSet() {
        Document existing = buildDoc(1L, 1L, "Old Name", DocumentType.ID_COPY);
        existing.setDescription("old desc");
        existing.setNotes("old notes");
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateDocumentRequest req = new UpdateDocumentRequest();
        req.setName("New Name");
        req.setType(DocumentType.TAX_CERTIFICATE);
        req.setDescription("new desc");
        req.setNotes("new notes");
        req.setVerified(true);

        Document updated = service.update(1L, req);

        assertEquals("New Name", updated.getName());
        assertEquals(DocumentType.TAX_CERTIFICATE, updated.getType());
        assertEquals("new desc", updated.getDescription());
        assertEquals("new notes", updated.getNotes());
        assertTrue(updated.isVerified());
    }

    @Test
    void update_nullFieldsNotChanged() {
        Document existing = buildDoc(1L, 1L, "Name", DocumentType.ID_COPY);
        existing.setDescription("desc");
        existing.setNotes("notes");
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateDocumentRequest req = new UpdateDocumentRequest();
        // all fields null

        Document updated = service.update(1L, req);

        assertEquals("Name", updated.getName());
        assertEquals(DocumentType.ID_COPY, updated.getType());
        assertEquals("desc", updated.getDescription());
        assertEquals("notes", updated.getNotes());
        assertFalse(updated.isVerified());
    }

    @Test
    void update_notFound_throwsDocumentNotFoundException() {
        when(repository.findById(999L)).thenReturn(Optional.empty());
        UpdateDocumentRequest req = new UpdateDocumentRequest();
        assertThrows(DocumentNotFoundException.class, () -> service.update(999L, req));
    }

    // â”€â”€ delete â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    void delete_success() {
        when(repository.existsById(1L)).thenReturn(true);
        assertDoesNotThrow(() -> service.delete(1L));
        verify(repository).deleteById(1L);
    }

    @Test
    void delete_notFound_throwsDocumentNotFoundException() {
        when(repository.existsById(999L)).thenReturn(false);
        assertThrows(DocumentNotFoundException.class, () -> service.delete(999L));
    }

    // â”€â”€ helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private Document buildDoc(Long id, Long appId, String name, DocumentType type) {
        Document d = new Document();
        d.setId(id);
        d.setApplicationId(appId);
        d.setName(name);
        d.setType(type);
        d.setVerified(false);
        return d;
    }
}
