package com.formforge.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Semester {
    I("I"),
    II("II");

    private final String value;

    Semester(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
