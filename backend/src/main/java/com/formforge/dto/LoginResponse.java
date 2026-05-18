package com.formforge.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Set;

@Data
@AllArgsConstructor
public class LoginResponse {
    private Long   id;
    private String username;
    private String email;
    /** Primary role name, e.g. "ADMIN" or "USER". */
    private String role;
    /** All permission names granted through the user's roles. */
    private Set<String> permissions;
    /** Signed JWT – must be sent as {@code Authorization: Bearer <token>}. */
    private String token;
}
