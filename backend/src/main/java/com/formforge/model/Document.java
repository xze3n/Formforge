package com.formforge.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Document {
    private Long id;
    private Long applicationId;
    private String name;
    private DocumentType type;
    private String description;
    private String dateAdded;
    private boolean verified;
    private String notes;
}
