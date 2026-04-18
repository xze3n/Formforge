package com.formforge.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Application {
    private Long id;
    private ApplicationType type;
    private String academicYear;
    private Semester semester;
    private String createdAt;
    private ApplicationStatus status;
}
