package com.formforge.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ApplicationStatus {
    DRAFT("Draft"),
    PENDING_ACTION("Pending Action"),
    APPROVED("Approved");

    private final String value;

    ApplicationStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
