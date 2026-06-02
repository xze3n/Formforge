package com.formforge.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyTwoFaRequest(
        @NotBlank String token
) {}
