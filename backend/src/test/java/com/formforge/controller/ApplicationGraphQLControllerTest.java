package com.formforge.controller;

import com.formforge.model.ApplicationStatus;
import com.formforge.model.ApplicationType;
import com.formforge.model.DocumentType;
import com.formforge.model.Semester;
import com.formforge.repository.InMemoryApplicationRepository;
import com.formforge.service.ApplicationGeneratorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.ExecutionGraphQlService;
import org.springframework.graphql.test.tester.ExecutionGraphQlServiceTester;
import org.springframework.graphql.test.tester.GraphQlTester;

import java.time.Year;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ApplicationGraphQLControllerTest {

    @Autowired
    private ExecutionGraphQlService graphQlService;

    private GraphQlTester graphQlTester;

    @Autowired
    private InMemoryApplicationRepository repository;

    @Autowired
    private ApplicationGeneratorService generatorService;

    @BeforeEach
    void setUp() {
        graphQlTester = ExecutionGraphQlServiceTester.create(graphQlService);
        repository.clear();
        generatorService.stop();
    }

    // ── Query: applications ────────────────────────────────────────────

    @Test
    void applications_empty() {
        graphQlTester.document("{ applications(page: 0, size: 5) { content { id } totalElements totalPages } }")
                .execute()
                .path("applications.content").entityList(Object.class).hasSize(0)
                .path("applications.totalElements").entity(Integer.class).isEqualTo(0)
                .path("applications.totalPages").entity(Integer.class).isEqualTo(0);
    }

    @Test
    void applications_defaultPagination() {
        seedApplications(8);

        graphQlTester.document("{ applications(page: 0, size: 5) { content { id } page size totalElements totalPages } }")
                .execute()
                .path("applications.content").entityList(Object.class).hasSize(5)
                .path("applications.page").entity(Integer.class).isEqualTo(0)
                .path("applications.size").entity(Integer.class).isEqualTo(5)
                .path("applications.totalElements").entity(Integer.class).isEqualTo(8)
                .path("applications.totalPages").entity(Integer.class).isEqualTo(2);
    }

    @Test
    void applications_secondPage() {
        seedApplications(8);

        graphQlTester.document("{ applications(page: 1, size: 5) { content { id } page } }")
                .execute()
                .path("applications.content").entityList(Object.class).hasSize(3)
                .path("applications.page").entity(Integer.class).isEqualTo(1);
    }

    @Test
    void applications_customPageSize() {
        seedApplications(8);

        graphQlTester.document("{ applications(page: 0, size: 3) { content { id } totalPages } }")
                .execute()
                .path("applications.content").entityList(Object.class).hasSize(3)
                .path("applications.totalPages").entity(Integer.class).isEqualTo(3);
    }

    @Test
    void applications_pageOutOfRange() {
        seedApplications(3);

        graphQlTester.document("{ applications(page: 10, size: 5) { content { id } totalElements } }")
                .execute()
                .path("applications.content").entityList(Object.class).hasSize(0)
                .path("applications.totalElements").entity(Integer.class).isEqualTo(3);
    }

    // ── Query: application (single) ────────────────────────────────────

    @Test
    void application_exists() {
        Long id = createAndGetId("Merit", "2025/2026", "I", null);

        graphQlTester.document("query($id: ID!) { application(id: $id) { id type academicYear semester status } }")
                .variable("id", id)
                .execute()
                .path("application.id").entity(String.class).isEqualTo(id.toString())
                .path("application.type").entity(String.class).isEqualTo("Merit")
                .path("application.academicYear").entity(String.class).isEqualTo("2025/2026")
                .path("application.semester").entity(String.class).isEqualTo("I")
                .path("application.status").entity(String.class).isEqualTo("Draft");
    }

    @Test
    void application_notFound() {
        graphQlTester.document("query { application(id: 999) { id } }")
                .execute()
                .errors()
                .expect(e -> e.getMessage().contains("999"))
                .verify()
                .path("application").valueIsNull();
    }

    // ── Query: enums ───────────────────────────────────────────────────

    @Test
    void enums_containsAllKeys() {
        graphQlTester.document("{ enums { types statuses semesters academicYears } }")
                .execute()
                .path("enums.types").entityList(String.class).hasSize(ApplicationType.values().length)
                .path("enums.statuses").entityList(String.class).hasSize(ApplicationStatus.values().length)
                .path("enums.semesters").entityList(String.class).hasSize(Semester.values().length)
                .path("enums.academicYears").entityList(String.class).hasSize(5);
    }

    @Test
    void enums_typesMatchEnum() {
        String[] expected = Arrays.stream(ApplicationType.values())
                .map(ApplicationType::getValue)
                .toArray(String[]::new);

        graphQlTester.document("{ enums { types } }")
                .execute()
                .path("enums.types").entityList(String.class)
                .satisfies(types -> assertThat(types).containsExactlyInAnyOrder(expected));
    }

    @Test
    void enums_statusesMatchEnum() {
        String[] expected = Arrays.stream(ApplicationStatus.values())
                .map(ApplicationStatus::getValue)
                .toArray(String[]::new);

        graphQlTester.document("{ enums { statuses } }")
                .execute()
                .path("enums.statuses").entityList(String.class)
                .satisfies(statuses -> assertThat(statuses).containsExactlyInAnyOrder(expected));
    }

    @Test
    void enums_semestersMatchEnum() {
        String[] expected = Arrays.stream(Semester.values())
                .map(Semester::getValue)
                .toArray(String[]::new);

        graphQlTester.document("{ enums { semesters } }")
                .execute()
                .path("enums.semesters").entityList(String.class)
                .satisfies(semesters -> assertThat(semesters).containsExactlyInAnyOrder(expected));
    }

    @Test
    void enums_documentTypesMatchEnum() {
        String[] expected = Arrays.stream(DocumentType.values())
                .map(DocumentType::getValue)
                .toArray(String[]::new);

        graphQlTester.document("{ enums { documentTypes } }")
                .execute()
                .path("enums.documentTypes").entityList(String.class)
                .satisfies(types -> assertThat(types).containsExactlyInAnyOrder(expected));
    }

    @Test
    void enums_academicYearsBasedOnCurrentYear() {
        int currentYear = Year.now().getValue();
        String first = (currentYear - 3) + "/" + (currentYear - 2);
        String last = (currentYear + 1) + "/" + (currentYear + 2);

        graphQlTester.document("{ enums { academicYears } }")
                .execute()
                .path("enums.academicYears").entityList(String.class)
                .satisfies(years -> {
                    assertThat(years).hasSize(5);
                    assertThat(years.get(0)).isEqualTo(first);
                    assertThat(years.get(4)).isEqualTo(last);
                });
    }

    // ── Query: generatorStatus ─────────────────────────────────────────

    @Test
    void generatorStatus_whenNotRunning() {
        graphQlTester.document("{ generatorStatus { running } }")
                .execute()
                .path("generatorStatus.running").entity(Boolean.class).isEqualTo(false);
    }

    @Test
    void generatorStatus_whenRunning() {
        generatorService.start();

        graphQlTester.document("{ generatorStatus { running } }")
                .execute()
                .path("generatorStatus.running").entity(Boolean.class).isEqualTo(true);
    }

    // ── Mutation: createApplication ────────────────────────────────────

    @Test
    void createApplication_valid() {
        String mutation = """
                mutation {
                    createApplication(input: { type: "Social", academicYear: "2024/2025", semester: "II" }) {
                        id type academicYear semester status createdAt
                    }
                }
                """;

        graphQlTester.document(mutation)
                .execute()
                .path("createApplication.id").hasValue()
                .path("createApplication.type").entity(String.class).isEqualTo("Social")
                .path("createApplication.academicYear").entity(String.class).isEqualTo("2024/2025")
                .path("createApplication.semester").entity(String.class).isEqualTo("II")
                .path("createApplication.status").entity(String.class).isEqualTo("Draft")
                .path("createApplication.createdAt").hasValue();
    }

    @Test
    void createApplication_withExplicitStatus() {
        String mutation = """
                mutation {
                    createApplication(input: { type: "Performance", academicYear: "2023/2024", semester: "I", status: "Approved" }) {
                        status
                    }
                }
                """;

        graphQlTester.document(mutation)
                .execute()
                .path("createApplication.status").entity(String.class).isEqualTo("Approved");
    }

    @Test
    void createApplication_invalidAcademicYearFormat() {
        String mutation = """
                mutation {
                    createApplication(input: { type: "Merit", academicYear: "2025", semester: "I" }) {
                        id
                    }
                }
                """;

        graphQlTester.document(mutation)
                .execute()
                .errors()
                .expect(e -> e.getMessage().contains("Academic year must be in format YYYY/YYYY"))
                .verify();
    }

    @Test
    void createApplication_enumSerialization() {
        String mutation = """
                mutation {
                    createApplication(input: { type: "Performance", academicYear: "2025/2026", semester: "I", status: "Pending Action" }) {
                        type status
                    }
                }
                """;

        graphQlTester.document(mutation)
                .execute()
                .path("createApplication.type").entity(String.class).isEqualTo("Performance")
                .path("createApplication.status").entity(String.class).isEqualTo("Pending Action");
    }

    // ── Mutation: updateApplication ────────────────────────────────────

    @Test
    void updateApplication_allFields() {
        Long id = createAndGetId("Merit", "2025/2026", "I", null);

        graphQlTester.document("""
                        mutation($id: ID!) {
                            updateApplication(id: $id, input: { type: "Social", academicYear: "2024/2025", semester: "II", status: "Approved" }) {
                                type academicYear semester status
                            }
                        }
                        """)
                .variable("id", id)
                .execute()
                .path("updateApplication.type").entity(String.class).isEqualTo("Social")
                .path("updateApplication.academicYear").entity(String.class).isEqualTo("2024/2025")
                .path("updateApplication.semester").entity(String.class).isEqualTo("II")
                .path("updateApplication.status").entity(String.class).isEqualTo("Approved");
    }

    @Test
    void updateApplication_partialFields() {
        Long id = createAndGetId("Merit", "2025/2026", "I", null);

        graphQlTester.document("""
                        mutation($id: ID!) {
                            updateApplication(id: $id, input: { academicYear: "2024/2025" }) {
                                type academicYear semester
                            }
                        }
                        """)
                .variable("id", id)
                .execute()
                .path("updateApplication.type").entity(String.class).isEqualTo("Merit")
                .path("updateApplication.academicYear").entity(String.class).isEqualTo("2024/2025")
                .path("updateApplication.semester").entity(String.class).isEqualTo("I");
    }

    @Test
    void updateApplication_invalidAcademicYearFormat() {
        Long id = createAndGetId("Merit", "2025/2026", "I", null);

        graphQlTester.document("""
                        mutation($id: ID!) {
                            updateApplication(id: $id, input: { academicYear: "bad" }) {
                                id
                            }
                        }
                        """)
                .variable("id", id)
                .execute()
                .errors()
                .expect(e -> e.getMessage().contains("Academic year must be in format YYYY/YYYY"))
                .verify();
    }

    @Test
    void updateApplication_notFound() {
        graphQlTester.document("""
                        mutation {
                            updateApplication(id: 999, input: { academicYear: "2024/2025" }) {
                                id
                            }
                        }
                        """)
                .execute()
                .errors()
                .expect(e -> e.getMessage().contains("999"))
                .verify();
    }

    // ── Mutation: deleteApplication ────────────────────────────────────

    @Test
    void deleteApplication_exists() {
        Long id = createAndGetId("Merit", "2025/2026", "I", null);

        graphQlTester.document("mutation($id: ID!) { deleteApplication(id: $id) }")
                .variable("id", id)
                .execute()
                .path("deleteApplication").entity(Boolean.class).isEqualTo(true);

        // Verify it's gone
        graphQlTester.document("query($id: ID!) { application(id: $id) { id } }")
                .variable("id", id)
                .execute()
                .errors()
                .expect(e -> e.getMessage().contains(id.toString()))
                .verify();
    }

    @Test
    void deleteApplication_notFound() {
        graphQlTester.document("mutation { deleteApplication(id: 999) }")
                .execute()
                .errors()
                .expect(e -> e.getMessage().contains("999"))
                .verify();
    }

    // ── Mutation: startGenerator / stopGenerator ───────────────────────

    @Test
    void startGenerator_returnsRunningTrue() {
        graphQlTester.document("mutation { startGenerator { running } }")
                .execute()
                .path("startGenerator.running").entity(Boolean.class).isEqualTo(true);
    }

    @Test
    void stopGenerator_returnsRunningFalse() {
        generatorService.start();

        graphQlTester.document("mutation { stopGenerator { running } }")
                .execute()
                .path("stopGenerator.running").entity(Boolean.class).isEqualTo(false);
    }

    // ── helpers ────────────────────────────────────────────────────────

    private Long createAndGetId(String type, String academicYear, String semester, String status) {
        String statusArg = status != null ? ", status: \"" + status + "\"" : "";
        String mutation = String.format("""
                mutation {
                    createApplication(input: { type: "%s", academicYear: "%s", semester: "%s"%s }) {
                        id
                    }
                }
                """, type, academicYear, semester, statusArg);

        return graphQlTester.document(mutation)
                .execute()
                .path("createApplication.id").entity(Long.class).get();
    }

    private void seedApplications(int count) {
        for (int i = 0; i < count; i++) {
            createAndGetId("Merit", "2025/2026", "I", null);
        }
    }
}
