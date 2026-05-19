package com.formforge.service;

import com.formforge.dto.ForgotPasswordRequest;
import com.formforge.dto.LoginRequest;
import com.formforge.dto.LoginResponse;
import com.formforge.dto.RefreshTokenRequest;
import com.formforge.dto.RegisterRequest;
import com.formforge.dto.ResetPasswordRequest;
import com.formforge.model.AuditAction;
import com.formforge.model.PasswordResetToken;
import com.formforge.model.Permission;
import com.formforge.model.RefreshToken;
import com.formforge.model.Role;
import com.formforge.model.User;
import com.formforge.repository.PasswordResetTokenRepository;
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

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository               userRepository;
    @Mock private RoleRepository               roleRepository;
    @Mock private AuditLogService              auditLogService;
    @Mock private PasswordEncoder              passwordEncoder;
    @Mock private JwtUtil                      jwtUtil;
    @Mock private RefreshTokenService          refreshTokenService;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;

    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(userRepository, roleRepository, auditLogService,
                passwordEncoder, jwtUtil, refreshTokenService, passwordResetTokenRepository);
        lenient().when(jwtUtil.generateToken(anyLong(), anyString(), anyString(), any()))
                .thenReturn("test.jwt.token");
        lenient().when(refreshTokenService.createRefreshToken(anyLong()))
                .thenReturn(buildRefreshToken(1L, "test.refresh.token"));
    }

    @AfterEach
    void cleanupRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    // login

    @Test
    void login_withEmail_success_returnsResponse_andLogsSuccess() {
        User user = buildUser(1L, "alice", "alice@test.com", "$2a$10$hashed", "USER");
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "$2a$10$hashed")).thenReturn(true);

        LoginRequest req = new LoginRequest();
        req.setIdentifier("alice@test.com");
        req.setPassword("secret");

        LoginResponse resp = service.login(req);

        assertEquals(1L, resp.getId());
        assertEquals("alice", resp.getUsername());
        assertEquals("USER",  resp.getRole());
        assertEquals("test.jwt.token",     resp.getToken());
        assertEquals("test.refresh.token", resp.getRefreshToken());

        verify(auditLogService).logAuth(eq(1L), eq("alice"), eq("USER"),
                eq(AuditAction.LOGIN_SUCCESS), isNull(), anyString(), eq(true));
    }

    @Test
    void login_withUsername_fallback_success() {
        User user = buildUser(2L, "bob", "bob@test.com", "$2a$10$hashed", "USER");
        when(userRepository.findByEmail("bob")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "$2a$10$hashed")).thenReturn(true);

        LoginRequest req = new LoginRequest();
        req.setIdentifier("bob");
        req.setPassword("secret");

        LoginResponse resp = service.login(req);

        assertEquals(2L, resp.getId());
        assertEquals("bob", resp.getUsername());
    }

    @Test
    void login_unknownIdentifier_throwsUnauthorized_andLogsFailure() {
        when(userRepository.findByEmail("ghost")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        LoginRequest req = new LoginRequest();
        req.setIdentifier("ghost");
        req.setPassword("pass");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.login(req));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());

        verify(auditLogService).logAuth(isNull(), eq("ghost"), isNull(),
                eq(AuditAction.LOGIN_FAILURE), eq("Unknown identifier"), anyString(), eq(false));
    }

    @Test
    void login_wrongPassword_throwsUnauthorized_andLogsFailure() {
        User user = buildUser(2L, "bob", "bob@test.com", "$2a$10$hashed", "USER");
        when(userRepository.findByEmail("bob@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPass", "$2a$10$hashed")).thenReturn(false);

        LoginRequest req = new LoginRequest();
        req.setIdentifier("bob@test.com");
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
        req.setIdentifier("alice@test.com");
        req.setPassword("secret");
        service.login(req);

        verify(auditLogService).logAuth(anyLong(), anyString(), anyString(),
                eq(AuditAction.LOGIN_SUCCESS), isNull(), eq("203.0.113.1"), eq(true));
    }

    @Test
    void login_usesUnknownIp_whenNoRequestContext() {
        User user = buildUser(1L, "alice", "alice@test.com", "$2a$10$hashed", "USER");
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        LoginRequest req = new LoginRequest();
        req.setIdentifier("alice@test.com");
        req.setPassword("secret");
        service.login(req);

        verify(auditLogService).logAuth(anyLong(), anyString(), anyString(),
                eq(AuditAction.LOGIN_SUCCESS), isNull(), eq("unknown"), eq(true));
    }

    // register

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
        assertNotNull(resp.getRefreshToken());
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

    // refreshToken

    @Test
    void refreshToken_validToken_returnsNewResponse() {
        RefreshToken old = buildRefreshToken(1L, "old-refresh-token");
        User user = buildUser(1L, "alice", "alice@test.com", "$2a$10$hashed", "USER");

        when(refreshTokenService.validateRefreshToken("old-refresh-token")).thenReturn(old);
        when(refreshTokenService.createRefreshToken(1L))
                .thenReturn(buildRefreshToken(2L, "new-refresh-token"));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken("old-refresh-token");

        LoginResponse resp = service.refreshToken(req);

        assertEquals("test.jwt.token", resp.getToken());
        assertEquals("new-refresh-token", resp.getRefreshToken());
        verify(refreshTokenService).revokeToken("old-refresh-token");
    }

    // logout

    @Test
    void logout_revokesRefreshToken() {
        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken("some-token");

        service.logout(req);

        verify(refreshTokenService).revokeToken("some-token");
    }

    // forgotPassword

    @Test
    void forgotPassword_knownEmail_returnsResetToken() {
        User user = buildUser(1L, "alice", "alice@test.com", "$2a$10$hashed", "USER");
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ForgotPasswordRequest req = new ForgotPasswordRequest();
        req.setEmail("alice@test.com");

        Map<String, String> result = service.forgotPassword(req);

        assertTrue(result.containsKey("resetToken"));
        verify(passwordResetTokenRepository).deleteByUserId(1L);
    }

    @Test
    void forgotPassword_unknownEmail_returnsGenericMessage() {
        when(userRepository.findByEmail("nobody@test.com")).thenReturn(Optional.empty());

        ForgotPasswordRequest req = new ForgotPasswordRequest();
        req.setEmail("nobody@test.com");

        Map<String, String> result = service.forgotPassword(req);

        assertFalse(result.containsKey("resetToken"));
        assertTrue(result.get("message").contains("registered"));
    }

    // resetPassword

    @Test
    void resetPassword_validToken_updatesPassword() {
        PasswordResetToken token = buildResetToken(1L, "reset-tok", false,
                Instant.now().plusSeconds(300));
        User user = buildUser(1L, "alice", "alice@test.com", "$2a$10$old", "USER");

        when(passwordResetTokenRepository.findByToken("reset-tok")).thenReturn(Optional.of(token));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newPass1")).thenReturn("$2a$10$new");

        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setToken("reset-tok");
        req.setNewPassword("newPass1");

        service.resetPassword(req);

        verify(passwordEncoder).encode("newPass1");
        verify(userRepository).save(user);
        verify(refreshTokenService).revokeAllForUser(1L);
    }

    @Test
    void resetPassword_usedToken_throwsBadRequest() {
        PasswordResetToken token = buildResetToken(1L, "used-tok", true,
                Instant.now().plusSeconds(300));
        when(passwordResetTokenRepository.findByToken("used-tok")).thenReturn(Optional.of(token));

        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setToken("used-tok");
        req.setNewPassword("pass1234");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.resetPassword(req));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void resetPassword_expiredToken_throwsBadRequest() {
        PasswordResetToken token = buildResetToken(1L, "exp-tok", false,
                Instant.now().minusSeconds(1));
        when(passwordResetTokenRepository.findByToken("exp-tok")).thenReturn(Optional.of(token));

        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setToken("exp-tok");
        req.setNewPassword("pass1234");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.resetPassword(req));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    // helpers

    private User buildUser(Long id, String username, String email, String password, String roleName) {
        Permission perm = new Permission(1L, "READ_APPLICATIONS");
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
        return new Role(1L, name, Set.of(new Permission(1L, "READ_APPLICATIONS")));
    }

    private RefreshToken buildRefreshToken(Long id, String token) {
        RefreshToken rt = new RefreshToken();
        rt.setId(id);
        rt.setUserId(1L);
        rt.setToken(token);
        rt.setExpiresAt(Instant.now().plusSeconds(7 * 24 * 3600));
        return rt;
    }

    private PasswordResetToken buildResetToken(Long userId, String token, boolean used, Instant expiresAt) {
        PasswordResetToken prt = new PasswordResetToken();
        prt.setUserId(userId);
        prt.setToken(token);
        prt.setUsed(used);
        prt.setExpiresAt(expiresAt);
        return prt;
    }
}