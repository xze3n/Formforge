package com.formforge.controller;

import com.formforge.repository.InMemoryDocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.ExecutionGraphQlService;
import org.springframework.graphql.test.tester.ExecutionGraphQlServiceTester;
import org.springframework.graphql.test.tester.GraphQlTester;

@SpringBootTest
class DocumentGraphQLControllerTest {

    @Autowired
    private ExecutionGraphQlService graphQlService;

    @Autowired
    private InMemoryDocumentRepository documentRepository;

    private GraphQlTester graphQlTester;

    @BeforeEach
    void setUp() {
        graphQlTester = ExecutionGraphQlServiceTester.create(graphQlService);
        documentRepository.clear();
    }

    // ── Query: documents ───────────────────────────────────────────────

    @Test
    void documents_emptyForApp() {
        graphQlTester.document("query { documents(applicationId: 99) { id name } }")
                .execute()
                .path("documents").entityList(Object.class).hasSize(0);
    }

    @Test
    void documents_returnsAllForApp() {
        graphQlTester.document("""
                mutation {
                    createDocument(applicationId: 1, input: { name: "Doc A", type: "ID Copy" }) { id }
                }""").execute();
        graphQlTester.document("""
                mutation {
                    createDocument(applicationId: 1, input: { name: "Doc B", type: "Tax Certificate" }) { id }
                }""").execute();
        graphQlTester.document("""
                mutation {
                    createDocument(applicationId: 2, input: { name: "Doc C", type: "Income Certificate" }) { id }
                }""").execute();

        graphQlTester.document("query { documents(applicationId: 1) { id name } }")
                .execute()
                .path("documents").entityList(Object.class).hasSize(2);
    }

    // ── Query: document (single) ───────────────────────────────────────

    @Test
    void document_found() {
        String id = graphQlTester.document("""
                mutation {
                    createDocument(applicationId: 1, input: {
                        name: "Enrollment",
                        type: "Student Enrollment Certificate",
                        description: "Official enrollment",
                        notes: "Reviewed"
                    }) { id name type description notes verified dateAdded applicationId }
                }""")
                .execute()
                .path("createDocument.id").entity(String.class).get();

        graphQlTester.document("query($id: ID!) { document(id: $id) { id name type applicationId verified } }")
                .variable("id", id)
                .execute()
                .path("document.id").entity(String.class).isEqualTo(id)
                .path("document.name").entity(String.class).isEqualTo("Enrollment")
                .path("document.type").entity(String.class).isEqualTo("Student Enrollment Certificate")
                .path("document.applicationId").entity(String.class).isEqualTo("1")
                .path("document.verified").entity(Boolean.class).isEqualTo(false);
    }

    @Test
    void document_notFound() {
        graphQlTester.document("query { document(id: 999) { id } }")
                .execute()
                .errors()
                .expect(e -> e.getMessage().contains("999"))
                .verify()
                .path("document").valueIsNull();
    }

    // ── Mutation: createDocument ───────────────────────────────────────

    @Test
    void createDocument_allFields() {
        graphQlTester.document("""
                mutation {
                    createDocument(applicationId: 2, input: {
                        name: "Income Proof",
                        type: "Income Certificate",
                        description: "Income proof doc",
                        notes: "Needs stamp"
                    }) { id name type description notes verified dateAdded applicationId }
                }""")
                .execute()
                .path("createDocument.id").hasValue()
                .path("createDocument.name").entity(String.class).isEqualTo("Income Proof")
                .path("createDocument.type").entity(String.class).isEqualTo("Income Certificate")
                .path("createDocument.description").entity(String.class).isEqualTo("Income proof doc")
                .path("createDocument.notes").entity(String.class).isEqualTo("Needs stamp")
                .path("createDocument.verified").entity(Boolean.class).isEqualTo(false)
                .path("createDocument.dateAdded").hasValue()
                .path("createDocument.applicationId").entity(String.class).isEqualTo("2");
    }

    @Test
    void createDocument_withoutOptionalFields() {
        graphQlTester.document("""
                mutation {
                    createDocument(applicationId: 3, input: { name: "Birth Cert", type: "Birth Certificate" }) {
                        id name type verified
                    }
                }""")
                .execute()
                .path("createDocument.id").hasValue()
                .path("createDocument.name").entity(String.class).isEqualTo("Birth Cert")
                .path("createDocument.type").entity(String.class).isEqualTo("Birth Certificate")
                .path("createDocument.verified").entity(Boolean.class).isEqualTo(false);
    }

    @Test
    void createDocument_invalidType_returnsError() {
        graphQlTester.document("""
                mutation {
                    createDocument(applicationId: 1, input: { name: "Doc", type: "Not A Valid Type" }) { id }
                }""")
                .execute()
                .errors()
                .expect(e -> e.getMessage() != null)
                .verify();
    }

    // ── Mutation: updateDocument ───────────────────────────────────────

    @Test
    void updateDocument_allFields() {
        String id = graphQlTester.document("""
                mutation {
                    createDocument(applicationId: 1, input: { name: "Old", type: "ID Copy" }) { id }
                }""")
                .execute()
                .path("createDocument.id").entity(String.class).get();

        graphQlTester.document("""
                mutation($id: ID!) {
                    updateDocument(id: $id, input: {
                        name: "New",
                        type: "Tax Certificate",
                        description: "Updated desc",
                        notes: "Updated notes",
                        verified: true
                    }) { id name type description notes verified }
                }""")
                .variable("id", id)
                .execute()
                .path("updateDocument.name").entity(String.class).isEqualTo("New")
                .path("updateDocument.type").entity(String.class).isEqualTo("Tax Certificate")
                .path("updateDocument.description").entity(String.class).isEqualTo("Updated desc")
                .path("updateDocument.notes").entity(String.class).isEqualTo("Updated notes")
                .path("updateDocument.verified").entity(Boolean.class).isEqualTo(true);
    }

    @Test
    void updateDocument_partialUpdate_unchangedFieldsPreserved() {
        String id = graphQlTester.document("""
                mutation {
                    createDocument(applicationId: 1, input: { name: "Original", type: "ID Copy" }) { id }
                }""")
                .execute()
                .path("createDocument.id").entity(String.class).get();

        graphQlTester.document("""
                mutation($id: ID!) {
                    updateDocument(id: $id, input: { verified: true }) {
                        name verified
                    }
                }""")
                .variable("id", id)
                .execute()
                .path("updateDocument.name").entity(String.class).isEqualTo("Original")
                .path("updateDocument.verified").entity(Boolean.class).isEqualTo(true);
    }

    @Test
    void updateDocument_notFound_returnsError() {
        graphQlTester.document("""
                mutation {
                    updateDocument(id: 999, input: { name: "X" }) { id }
                }""")
                .execute()
                .errors()
                .expect(e -> e.getMessage() != null)
                .verify();
    }

    // ── Mutation: deleteDocument ───────────────────────────────────────

    @Test
    void deleteDocument_success() {
        String id = graphQlTester.document("""
                mutation {
                    createDocument(applicationId: 1, input: { name: "Temp", type: "ID Copy" }) { id }
                }""")
                .execute()
                .path("createDocument.id").entity(String.class).get();

        graphQlTester.document("mutation($id: ID!) { deleteDocument(id: $id) }")
                .variable("id", id)
                .execute()
                .path("deleteDocument").entity(Boolean.class).isEqualTo(true);

        // Verify gone
        graphQlTester.document("query($id: ID!) { document(id: $id) { id } }")
                .variable("id", id)
                .execute()
                .errors()
                .expect(e -> e.getMessage() != null)
                .verify();
    }

    @Test
    void deleteDocument_notFound_returnsError() {
        graphQlTester.document("mutation { deleteDocument(id: 999) }")
                .execute()
                .errors()
                .expect(e -> e.getMessage() != null)
                .verify();
    }
}
