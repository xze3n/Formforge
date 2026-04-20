package com.formforge.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ApplicationType {
    MERIT("Merit"),
    SOCIAL("Social"),
    PERFORMANCE("Performance");

    private final String value;

    ApplicationType(String value) {
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

    public static ApplicationType fromValue(String value) {
        for (ApplicationType t : values()) {
            if (t.value.equalsIgnoreCase(value)) return t;
        }
        throw new IllegalArgumentException("Unknown ApplicationType: " + value);
    }
}
