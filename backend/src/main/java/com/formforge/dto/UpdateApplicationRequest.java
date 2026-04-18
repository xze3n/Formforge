package com.formforge.dto;

import com.formforge.model.ApplicationStatus;
import com.formforge.model.ApplicationType;
import com.formforge.model.Semester;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateApplicationRequest {

    private ApplicationType type;

    @Pattern(regexp = "^\\d{4}/\\d{4}$", message = "Academic year must be in format YYYY/YYYY")
    private String academicYear;

    private Semester semester;

    private ApplicationStatus status;
}
