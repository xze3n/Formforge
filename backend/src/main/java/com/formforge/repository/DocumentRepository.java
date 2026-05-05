package com.formforge.repository;

import com.formforge.model.Document;
import com.formforge.model.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findByApplicationId(Long applicationId);

    @Transactional
    void deleteByApplicationId(Long applicationId);

    List<Document> findByType(DocumentType type);

    List<Document> findByVerified(boolean verified);
}
