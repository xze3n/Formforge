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
}
