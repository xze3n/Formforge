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

    @Override
    public String toString() {
        return value;
    }

    public static Semester fromValue(String value) {
        for (Semester s : values()) {
            if (s.value.equalsIgnoreCase(value)) return s;
        }
        throw new IllegalArgumentException("Unknown Semester: " + value);
    }
}
