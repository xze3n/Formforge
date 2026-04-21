package com.formforge.controller;

import com.formforge.dto.CreateDocumentRequest;
import com.formforge.dto.UpdateDocumentRequest;
import com.formforge.model.Document;
import com.formforge.model.DocumentType;
import com.formforge.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class DocumentGraphQLController {

    private final DocumentService documentService;

    // ── Queries ────────────────────────────────────────────────────────

    @QueryMapping
    public List<Document> documents(@Argument Long applicationId) {
        return documentService.getByApplicationId(applicationId);
    }

    @QueryMapping
    public Document document(@Argument Long id) {
        return documentService.getById(id);
    }

    // ── Mutations ──────────────────────────────────────────────────────

    @MutationMapping
    public Document createDocument(@Argument Long applicationId, @Argument CreateDocumentInput input) {
        CreateDocumentRequest request = new CreateDocumentRequest();
        request.setName(input.name());
        request.setType(DocumentType.fromValue(input.type()));
        request.setDescription(input.description());
        request.setNotes(input.notes());
        return documentService.create(applicationId, request);
    }

    @MutationMapping
    public Document updateDocument(@Argument Long id, @Argument UpdateDocumentInput input) {
        UpdateDocumentRequest request = new UpdateDocumentRequest();
        if (input.name() != null) request.setName(input.name());
        if (input.type() != null) request.setType(DocumentType.fromValue(input.type()));
        if (input.description() != null) request.setDescription(input.description());
        if (input.notes() != null) request.setNotes(input.notes());
        if (input.verified() != null) request.setVerified(input.verified());
        return documentService.update(id, request);
    }

    @MutationMapping
    public boolean deleteDocument(@Argument Long id) {
        documentService.delete(id);
        return true;
    }

    // ── Input record types ─────────────────────────────────────────────

    public record CreateDocumentInput(String name, String type, String description, String notes) {}
    public record UpdateDocumentInput(String name, String type, String description, String notes, Boolean verified) {}
}
