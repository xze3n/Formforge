package com.formforge.controller;

import com.formforge.dto.LoginRequest;
import com.formforge.dto.LoginResponse;
import com.formforge.dto.RegisterRequest;
import com.formforge.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

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
                Set.of("READ"), "test.jwt.token");
        when(userService.login(any())).thenReturn(resp);

        LoginRequest req = new LoginRequest();
        req.setEmail("alice@test.com");
        req.setPassword("secret");

        ResponseEntity<LoginResponse> result = controller.login(req);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals("alice", result.getBody().getUsername());
        verify(userService).login(req);
    }

    @Test
    void register_returnsCreatedWithResponse() {
        LoginResponse resp = new LoginResponse(2L, "bob", "bob@test.com", "USER",
                Set.of(), "test.jwt.token");
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
}
