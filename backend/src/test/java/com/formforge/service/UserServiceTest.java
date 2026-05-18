package com.formforge.service;

import com.formforge.dto.LoginRequest;
import com.formforge.dto.LoginResponse;
import com.formforge.dto.RegisterRequest;
import com.formforge.model.AuditAction;
import com.formforge.model.Permission;
import com.formforge.model.Role;
import com.formforge.model.User;
import com.formforge.repository.RoleRepository;
import com.formforge.repository.UserRepository;
import com.formforge.security.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository   userRepository;
    @Mock private RoleRepository   roleRepository;
    @Mock private AuditLogService  auditLogService;
    @Mock private PasswordEncoder  passwordEncoder;
    @Mock private JwtUtil          jwtUtil;

    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(userRepository, roleRepository, auditLogService,
                                  passwordEncoder, jwtUtil);
        // Default: JWT util always returns a test token (lenient: not all tests reach this call)
        lenient().when(jwtUtil.generateToken(anyLong(), anyString(), anyString()))
                 .thenReturn("test.jwt.token");
    }

    @AfterEach
    void cleanupRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    // â”€â”€ login â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    void login_success_returnsResponse_withToken_andLogsSuccess() {
        User user = buildUser(1L, "alice", "alice@test.com", "$2a$10$hashed", "USER");
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "$2a$10$hashed")).thenReturn(true);

        LoginRequest req = new LoginRequest();
        req.setEmail("alice@test.com");
        req.setPassword("secret");

        LoginResponse resp = service.login(req);

        assertEquals(1L, resp.getId());
        assertEquals("alice", resp.getUsername());
        assertEquals("USER",  resp.getRole());
        assertEquals("test.jwt.token", resp.getToken());

        verify(auditLogService).logAuth(eq(1L), eq("alice"), eq("USER"),
                eq(AuditAction.LOGIN_SUCCESS), isNull(), anyString(), eq(true));
    }

    @Test
    void login_unknownEmail_throwsUnauthorized_andLogsFailure() {
        when(userRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

        LoginRequest req = new LoginRequest();
        req.setEmail("ghost@test.com");
        req.setPassword("pass");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.login(req));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());

        verify(auditLogService).logAuth(isNull(), eq("ghost@test.com"), isNull(),
                eq(AuditAction.LOGIN_FAILURE), eq("Unknown email"), anyString(), eq(false));
    }

    @Test
    void login_wrongPassword_throwsUnauthorized_andLogsFailure() {
        User user = buildUser(2L, "bob", "bob@test.com", "$2a$10$hashed", "USER");
        when(userRepository.findByEmail("bob@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPass", "$2a$10$hashed")).thenReturn(false);

        LoginRequest req = new LoginRequest();
        req.setEmail("bob@test.com");
        req.setPassword("wrongPass");

        assertThrows(ResponseStatusException.class, () -> service.login(req));

        verify(auditLogService).logAuth(eq(2L), eq("bob"), eq("USER"),
                eq(AuditAction.LOGIN_FAILURE), eq("Wrong password"), anyString(), eq(false));
    }

    @Test
    void login_usesXForwardedFor_whenPresent() {
        User user = buildUser(1L, "alice", "alice@test.com", "$2a$10$hashed", "USER");
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        MockHttpServletRequest httpReq = new MockHttpServletRequest();
        httpReq.addHeader("X-Forwarded-For", "203.0.113.1, 10.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(httpReq));

        LoginRequest req = new LoginRequest();
        req.setEmail("alice@test.com");
        req.setPassword("secret");
        service.login(req);

        verify(auditLogService).logAuth(anyLong(), anyString(), anyString(),
                eq(AuditAction.LOGIN_SUCCESS), isNull(), eq("203.0.113.1"), eq(true));
    }

    @Test
    void login_usesRemoteAddr_whenNoXff() {
        User user = buildUser(1L, "alice", "alice@test.com", "$2a$10$hashed", "USER");
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        MockHttpServletRequest httpReq = new MockHttpServletRequest();
        httpReq.setRemoteAddr("192.168.1.50");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(httpReq));

        LoginRequest req = new LoginRequest();
        req.setEmail("alice@test.com");
        req.setPassword("secret");
        service.login(req);

        verify(auditLogService).logAuth(anyLong(), anyString(), anyString(),
                eq(AuditAction.LOGIN_SUCCESS), isNull(), eq("192.168.1.50"), eq(true));
    }

    @Test
    void login_usesUnknownIp_whenNoRequestContext() {
        User user = buildUser(1L, "alice", "alice@test.com", "$2a$10$hashed", "USER");
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        LoginRequest req = new LoginRequest();
        req.setEmail("alice@test.com");
        req.setPassword("secret");
        service.login(req);

        verify(auditLogService).logAuth(anyLong(), anyString(), anyString(),
                eq(AuditAction.LOGIN_SUCCESS), isNull(), eq("unknown"), eq(true));
    }

    // â”€â”€ register â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    void register_success_encodesPassword_savesUser_andLogsRegister() {
        Role userRole = buildRole("USER");
        when(userRepository.existsByEmail("carol@test.com")).thenReturn(false);
        when(userRepository.existsByUsername("carol")).thenReturn(false);
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("pass")).thenReturn("$2a$10$encoded");
        User saved = buildUser(5L, "carol", "carol@test.com", "$2a$10$encoded", "USER");
        when(userRepository.save(any())).thenReturn(saved);

        RegisterRequest req = new RegisterRequest();
        req.setUsername("carol");
        req.setEmail("carol@test.com");
        req.setPassword("pass");

        LoginResponse resp = service.register(req);

        assertEquals(5L, resp.getId());
        assertEquals("carol", resp.getUsername());
        assertEquals("test.jwt.token", resp.getToken());
        // Verify password was encoded before saving
        verify(passwordEncoder).encode("pass");
        verify(auditLogService).logAuth(eq(5L), eq("carol"), eq("USER"),
                eq(AuditAction.REGISTER), isNull(), anyString(), eq(true));
    }

    @Test
    void register_conflictEmail_throwsConflict() {
        when(userRepository.existsByEmail("dup@test.com")).thenReturn(true);

        RegisterRequest req = new RegisterRequest();
        req.setUsername("newuser");
        req.setEmail("dup@test.com");
        req.setPassword("pass");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.register(req));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void register_conflictUsername_throwsConflict() {
        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(userRepository.existsByUsername("taken")).thenReturn(true);

        RegisterRequest req = new RegisterRequest();
        req.setUsername("taken");
        req.setEmail("new@test.com");
        req.setPassword("pass");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.register(req));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void register_missingDefaultRole_throwsInternalServerError() {
        when(userRepository.existsByEmail("x@test.com")).thenReturn(false);
        when(userRepository.existsByUsername("x")).thenReturn(false);
        when(roleRepository.findByName("USER")).thenReturn(Optional.empty());

        RegisterRequest req = new RegisterRequest();
        req.setUsername("x");
        req.setEmail("x@test.com");
        req.setPassword("pass");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.register(req));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatusCode());
    }

    // â”€â”€ helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private User buildUser(Long id, String username, String email, String password, String roleName) {
        Permission perm = new Permission(1L, "READ");
        Role role = new Role(1L, roleName, Set.of(perm));
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(password);
        user.setRoles(Set.of(role));
        return user;
    }

    private Role buildRole(String name) {
        return new Role(1L, name, Set.of(new Permission(1L, "READ")));
    }
}

