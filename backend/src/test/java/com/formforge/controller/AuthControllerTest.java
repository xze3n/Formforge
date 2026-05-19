package com.formforge.controller;

import com.formforge.dto.ForgotPasswordRequest;
import com.formforge.dto.LoginRequest;
import com.formforge.dto.LoginResponse;
import com.formforge.dto.RefreshTokenRequest;
import com.formforge.dto.RegisterRequest;
import com.formforge.dto.ResetPasswordRequest;
import com.formforge.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserService userService;

    private AuthController controller;

    @BeforeEach
    void setUp() {
        controller = new AuthController(userService);
    }

    @Test
    void login_returnsOkWithResponse() {
        LoginResponse resp = new LoginResponse(1L, "alice", "alice@test.com", "USER",
                Set.of("READ_APPLICATIONS"), "test.jwt.token", "test.refresh.token");
        when(userService.login(any())).thenReturn(resp);

        LoginRequest req = new LoginRequest();
        req.setIdentifier("alice@test.com");
        req.setPassword("secret");

        ResponseEntity<LoginResponse> result = controller.login(req);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals("alice", result.getBody().getUsername());
        assertEquals("test.refresh.token", result.getBody().getRefreshToken());
        verify(userService).login(req);
    }

    @Test
    void register_returnsCreatedWithResponse() {
        LoginResponse resp = new LoginResponse(2L, "bob", "bob@test.com", "USER",
                Set.of(), "test.jwt.token", "test.refresh.token");
        when(userService.register(any())).thenReturn(resp);

        RegisterRequest req = new RegisterRequest();
        req.setUsername("bob");
        req.setEmail("bob@test.com");
        req.setPassword("pass1234");

        ResponseEntity<LoginResponse> result = controller.register(req);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals("bob", result.getBody().getUsername());
        verify(userService).register(req);
    }

    @Test
    void refresh_returnsOkWithNewTokens() {
        LoginResponse resp = new LoginResponse(1L, "alice", "alice@test.com", "USER",
                Set.of(), "new.jwt.token", "new.refresh.token");
        when(userService.refreshToken(any())).thenReturn(resp);

        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken("old.refresh.token");

        ResponseEntity<LoginResponse> result = controller.refresh(req);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("new.jwt.token", result.getBody().getToken());
        verify(userService).refreshToken(req);
    }

    @Test
    void logout_returnsOkWithMessage() {
        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken("some.refresh.token");

        ResponseEntity<Map<String, String>> result = controller.logout(req);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("Logged out successfully", result.getBody().get("message"));
        verify(userService).logout(req);
    }

    @Test
    void forgotPassword_returnsOkWithMessage() {
        when(userService.forgotPassword(any())).thenReturn(
                Map.of("message", "Token generated", "resetToken", "tok-123"));

        ForgotPasswordRequest req = new ForgotPasswordRequest();
        req.setEmail("alice@test.com");

        ResponseEntity<Map<String, String>> result = controller.forgotPassword(req);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("tok-123", result.getBody().get("resetToken"));
    }

    @Test
    void resetPassword_returnsOkWithMessage() {
        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setToken("tok-123");
        req.setNewPassword("newPass1234");

        ResponseEntity<Map<String, String>> result = controller.resetPassword(req);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("Password updated successfully", result.getBody().get("message"));
        verify(userService).resetPassword(req);
    }
}