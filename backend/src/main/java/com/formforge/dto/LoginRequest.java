package com.formforge.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    /**
     * Accepts either an e-mail address or a username.
     * UserService.login() tries e-mail first, then falls back to username.
     */
    @NotBlank
    private String identifier;

    @NotBlank
    private String password;
}
