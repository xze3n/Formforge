package com.formforge.controller;

import com.formforge.model.ApplicationStatus;
import com.formforge.model.ApplicationType;
import com.formforge.model.Semester;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Year;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

@RestController
@RequestMapping("/api/enums")
public class EnumController {

    @GetMapping
    public ResponseEntity<Map<String, List<String>>> getAll() {
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

        return ResponseEntity.ok(Map.of(
                "types", types,
                "statuses", statuses,
                "semesters", semesters,
                "academicYears", academicYears
        ));
    }
}
