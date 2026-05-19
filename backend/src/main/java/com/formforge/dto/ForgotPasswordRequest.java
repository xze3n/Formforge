package com.formforge.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForgotPasswordRequest {

    /** E-mail address of the account to reset. */
    @NotBlank
    private String email;
}
