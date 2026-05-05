package com.formforge.service;

import com.formforge.dto.CreateDocumentRequest;
import com.formforge.dto.UpdateDocumentRequest;
import com.formforge.exception.DocumentNotFoundException;
import com.formforge.model.Document;
import com.formforge.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository repository;

    public List<Document> getByApplicationId(Long applicationId) {
        return repository.findByApplicationId(applicationId);
    }

    public Document getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new DocumentNotFoundException(id));
    }

    public Document create(Long applicationId, CreateDocumentRequest request) {
        Document document = new Document();
        document.setApplicationId(applicationId);
        document.setName(request.getName());
        document.setType(request.getType());
        document.setDescription(request.getDescription());
        document.setNotes(request.getNotes());
        document.setVerified(false);
        return repository.save(document);
    }

    public Document update(Long id, UpdateDocumentRequest request) {
        Document existing = repository.findById(id)
                .orElseThrow(() -> new DocumentNotFoundException(id));

        if (request.getName() != null) existing.setName(request.getName());
        if (request.getType() != null) existing.setType(request.getType());
        if (request.getDescription() != null) existing.setDescription(request.getDescription());
        if (request.getNotes() != null) existing.setNotes(request.getNotes());
        if (request.getVerified() != null) existing.setVerified(request.getVerified());

        return repository.save(existing);
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new DocumentNotFoundException(id);
        }
        repository.deleteById(id);
    }
}
