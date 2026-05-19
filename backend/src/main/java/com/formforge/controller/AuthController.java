package com.formforge.controller;

import com.formforge.dto.ForgotPasswordRequest;
import com.formforge.dto.LoginRequest;
import com.formforge.dto.LoginResponse;
import com.formforge.dto.RefreshTokenRequest;
import com.formforge.dto.RegisterRequest;
import com.formforge.dto.ResetPasswordRequest;
import com.formforge.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    /** Authentication method 1 + 2: login with e-mail OR username + password. */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(userService.login(request));
    }

    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        LoginResponse response = userService.register(request);
        return ResponseEntity.status(201).body(response);
    }

    /** Authentication method 3: exchange a valid refresh token for a new access token. */
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(userService.refreshToken(request));
    }

    /** Invalidates the given refresh token, ending the session. */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@Valid @RequestBody RefreshTokenRequest request) {
        userService.logout(request);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    /** Password recovery step 1: request a reset token for the given e-mail. */
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(userService.forgotPassword(request));
    }

    /** Password recovery step 2: set a new password using the reset token. */
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(request);
        return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
    }
}
