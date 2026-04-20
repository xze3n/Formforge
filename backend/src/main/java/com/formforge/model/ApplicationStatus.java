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

    @Override
    public String toString() {
        return value;
    }

    public static ApplicationStatus fromValue(String value) {
        for (ApplicationStatus s : values()) {
            if (s.value.equalsIgnoreCase(value)) return s;
        }
        throw new IllegalArgumentException("Unknown ApplicationStatus: " + value);
    }
}
