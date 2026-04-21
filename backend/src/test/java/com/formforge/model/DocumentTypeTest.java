package com.formforge.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DocumentTypeTest {

    @Test
    void getValue_returnsDisplayName() {
        assertEquals("Student Enrollment Certificate", DocumentType.STUDENT_ENROLLMENT_CERTIFICATE.getValue());
        assertEquals("Social Assessment Report",       DocumentType.SOCIAL_ASSESSMENT_REPORT.getValue());
        assertEquals("Income Certificate",             DocumentType.INCOME_CERTIFICATE.getValue());
        assertEquals("Tax Certificate",                DocumentType.TAX_CERTIFICATE.getValue());
        assertEquals("ID Copy",                        DocumentType.ID_COPY.getValue());
        assertEquals("Birth Certificate",              DocumentType.BIRTH_CERTIFICATE.getValue());
        assertEquals("Medical Certificate",            DocumentType.MEDICAL_CERTIFICATE.getValue());
        assertEquals("Self-Declaration",               DocumentType.SELF_DECLARATION.getValue());
        assertEquals("Pension Slip",                   DocumentType.PENSION_SLIP.getValue());
        assertEquals("Death Certificate",              DocumentType.DEATH_CERTIFICATE.getValue());
    }

    @Test
    void toString_returnsDisplayName() {
        assertEquals("Tax Certificate",   DocumentType.TAX_CERTIFICATE.toString());
        assertEquals("ID Copy",           DocumentType.ID_COPY.toString());
        assertEquals("Pension Slip",      DocumentType.PENSION_SLIP.toString());
    }

    @Test
    void fromValue_exactMatch() {
        assertEquals(DocumentType.INCOME_CERTIFICATE, DocumentType.fromValue("Income Certificate"));
        assertEquals(DocumentType.DEATH_CERTIFICATE,  DocumentType.fromValue("Death Certificate"));
    }

    @Test
    void fromValue_caseInsensitive() {
        assertEquals(DocumentType.ID_COPY,             DocumentType.fromValue("id copy"));
        assertEquals(DocumentType.SELF_DECLARATION,    DocumentType.fromValue("SELF-DECLARATION"));
        assertEquals(DocumentType.MEDICAL_CERTIFICATE, DocumentType.fromValue("medical certificate"));
    }

    @Test
    void fromValue_invalidValue_throwsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> DocumentType.fromValue("Not A Valid Type")
        );
        assertTrue(ex.getMessage().contains("Not A Valid Type"));
    }
}
