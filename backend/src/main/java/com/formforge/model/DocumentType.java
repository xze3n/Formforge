package com.formforge.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum DocumentType {
    STUDENT_ENROLLMENT_CERTIFICATE("Student Enrollment Certificate"),
    SOCIAL_ASSESSMENT_REPORT("Social Assessment Report"),
    INCOME_CERTIFICATE("Income Certificate"),
    TAX_CERTIFICATE("Tax Certificate"),
    ID_COPY("ID Copy"),
    BIRTH_CERTIFICATE("Birth Certificate"),
    MEDICAL_CERTIFICATE("Medical Certificate"),
    SELF_DECLARATION("Self-Declaration"),
    PENSION_SLIP("Pension Slip"),
    DEATH_CERTIFICATE("Death Certificate");

    private final String value;

    DocumentType(String value) {
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

    public static DocumentType fromValue(String value) {
        for (DocumentType t : values()) {
            if (t.value.equalsIgnoreCase(value)) return t;
        }
        throw new IllegalArgumentException("Unknown DocumentType: " + value);
    }
}
