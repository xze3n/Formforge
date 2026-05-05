package com.formforge.controller;

import com.formforge.dto.CreateApplicationRequest;
import com.formforge.dto.PageResponse;
import com.formforge.dto.UpdateApplicationRequest;
import com.formforge.model.Application;
import com.formforge.model.ApplicationStatus;
import com.formforge.model.ApplicationType;
import com.formforge.model.DocumentType;
import com.formforge.model.Semester;
import com.formforge.service.ApplicationGeneratorService;
import com.formforge.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.time.Year;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

@Controller
@RequiredArgsConstructor
public class ApplicationGraphQLController {

    private final ApplicationService applicationService;
    private final ApplicationGeneratorService generatorService;

    // ── Queries ────────────────────────────────────────────────────────

    @QueryMapping
    public PageResponse<Application> applications(
            @Argument int page,
            @Argument int size,
            @Argument(name = "status") String status,
            @Argument(name = "type") String type) {

        if (status != null && !status.isBlank()) {
            return applicationService.getByStatus(ApplicationStatus.fromValue(status), page, size);
        }
        if (type != null && !type.isBlank()) {
            return applicationService.getByType(ApplicationType.fromValue(type), page, size);
        }
        return applicationService.getAll(page, size);
    }

    @QueryMapping
    public Application application(@Argument Long id) {
        return applicationService.getById(id);
    }

    @QueryMapping
    public Map<String, List<String>> enums() {
        int currentYear = Year.now().getValue();

        List<String> types = Arrays.stream(ApplicationType.values())
                .map(ApplicationType::getValue)
                .toList();

        List<String> statuses = Arrays.stream(ApplicationStatus.values())
                .map(ApplicationStatus::getValue)
                .toList();

        List<String> semesters = Arrays.stream(Semester.values())
                .map(Semester::getValue)
                .toList();

        List<String> academicYears = IntStream.rangeClosed(currentYear - 3, currentYear + 1)
                .mapToObj(y -> y + "/" + (y + 1))
                .toList();

        List<String> documentTypes = Arrays.stream(DocumentType.values())
                .map(DocumentType::getValue)
                .toList();

        return Map.of(
                "types", types,
                "statuses", statuses,
                "semesters", semesters,
                "academicYears", academicYears,
                "documentTypes", documentTypes
        );
    }

    @QueryMapping
    public Map<String, Boolean> generatorStatus() {
        return Map.of("running", generatorService.isRunning());
    }

    @QueryMapping
    public ApplicationStats statistics() {
        Map<String, Map<String, Long>> raw = applicationService.getStatistics();
        return new ApplicationStats(
                toStatEntries(raw.get("byStatus")),
                toStatEntries(raw.get("byType")),
                toStatEntries(raw.get("bySemester"))
        );
    }

    // ── Mutations ──────────────────────────────────────────────────────

    @MutationMapping
    public Application createApplication(@Argument CreateApplicationInput input) {
        validateAcademicYear(input.academicYear());
        CreateApplicationRequest request = new CreateApplicationRequest();
        request.setType(ApplicationType.fromValue(input.type()));
        request.setAcademicYear(input.academicYear());
        request.setSemester(Semester.fromValue(input.semester()));
        if (input.status() != null) {
            request.setStatus(ApplicationStatus.fromValue(input.status()));
        }
        return applicationService.create(request);
    }

    @MutationMapping
    public Application updateApplication(@Argument Long id, @Argument UpdateApplicationInput input) {
        if (input.academicYear() != null) validateAcademicYear(input.academicYear());
        UpdateApplicationRequest request = new UpdateApplicationRequest();
        if (input.type() != null) request.setType(ApplicationType.fromValue(input.type()));
        if (input.academicYear() != null) request.setAcademicYear(input.academicYear());
        if (input.semester() != null) request.setSemester(Semester.fromValue(input.semester()));
        if (input.status() != null) request.setStatus(ApplicationStatus.fromValue(input.status()));
        return applicationService.update(id, request);
    }

    @MutationMapping
    public boolean deleteApplication(@Argument Long id) {
        applicationService.delete(id);
        return true;
    }

    @MutationMapping
    public Map<String, Boolean> startGenerator() {
        generatorService.start();
        return Map.of("running", true);
    }

    @MutationMapping
    public Map<String, Boolean> stopGenerator() {
        generatorService.stop();
        return Map.of("running", false);
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private void validateAcademicYear(String academicYear) {
        if (!academicYear.matches("^\\d{4}/\\d{4}$")) {
            throw new IllegalArgumentException("Academic year must be in format YYYY/YYYY");
        }
    }

    private List<StatEntry> toStatEntries(Map<String, Long> map) {
        if (map == null) return List.of();
        return map.entrySet().stream()
                .map(e -> new StatEntry(e.getKey(), e.getValue()))
                .toList();
    }

    // ── Inner record types ─────────────────────────────────────────────

    public record CreateApplicationInput(String type, String academicYear, String semester, String status) {}
    public record UpdateApplicationInput(String type, String academicYear, String semester, String status) {}
    public record StatEntry(String key, Long count) {}
    public record ApplicationStats(List<StatEntry> byStatus, List<StatEntry> byType, List<StatEntry> bySemester) {}
}
