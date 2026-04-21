package com.formforge.dto;

import com.formforge.model.DocumentType;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CreateDocumentRequest {
    private String name;
    private DocumentType type;
    private String description;
    private String notes;
}
