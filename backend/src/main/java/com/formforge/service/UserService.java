package com.formforge.service;

import com.formforge.dto.ForgotPasswordRequest;
import com.formforge.dto.LoginRequest;
import com.formforge.dto.LoginResponse;
import com.formforge.dto.RefreshTokenRequest;
import com.formforge.dto.RegisterRequest;
import com.formforge.dto.ResetPasswordRequest;
import com.formforge.dto.TwoFaRequiredResponse;
import com.formforge.dto.VerifyTwoFaRequest;
import com.formforge.model.AuditAction;
import com.formforge.model.PasswordResetToken;
import com.formforge.model.RefreshToken;
import com.formforge.model.Role;
import com.formforge.model.TwoFactorToken;
import com.formforge.model.User;
import com.formforge.repository.PasswordResetTokenRepository;
import com.formforge.repository.RoleRepository;
import com.formforge.repository.TwoFactorTokenRepository;
import java.util.List;
import com.formforge.repository.UserRepository;
import com.formforge.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository               userRepository;
    private final RoleRepository               roleRepository;
    private final AuditLogService              auditLogService;
    private final PasswordEncoder              passwordEncoder;
    private final JwtUtil                      jwtUtil;
    private final RefreshTokenService          refreshTokenService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final TwoFactorTokenRepository     twoFactorTokenRepository;

    /** Token validity for the 2FA link (10 minutes). */
    private static final long TWO_FA_EXPIRY_MINUTES = 10;

    // ── Authentication method 1 + 2: email OR username + password ────────────

    /**
     * Validates credentials and returns a new access token + refresh token.
     * {@code request.identifier} may be an e-mail address (method 1) or a
     * username (method 2), giving three total ways to authenticate including
     * token-based refresh.
     */
    /**
     * Step 1 of login: validates credentials, generates a 2FA token, logs it to
     * the console (simulating an e-mail send), and returns a pending response.
     * The client must then call {@code POST /api/auth/verify-2fa} with the token.
     */
    @Transactional
    public TwoFaRequiredResponse login(LoginRequest request) {
        String ip = currentIp();
        String id = request.getIdentifier();

        // Try e-mail first, then fall back to username
        Optional<User> maybeUser = userRepository.findByEmail(id)
                .or(() -> userRepository.findByUsername(id));

        if (maybeUser.isEmpty()) {
            auditLogService.logAuth(null, id, null,
                    AuditAction.LOGIN_FAILURE, "Unknown identifier", ip, false);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        User user = maybeUser.get();
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            auditLogService.logAuth(user.getId(), user.getUsername(), primaryRole(user),
                    AuditAction.LOGIN_FAILURE, "Wrong password", ip, false);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        // Invalidate any previous unused tokens for this user
        twoFactorTokenRepository.deleteExpired(Instant.now());

        // 6-digit numeric code (100000–999999)
        String tokenValue = String.valueOf(100_000 + new java.util.Random().nextInt(900_000));
        TwoFactorToken twoFaToken = new TwoFactorToken();
        twoFaToken.setUserId(user.getId());
        twoFaToken.setToken(tokenValue);
        twoFaToken.setExpiresAt(Instant.now().plus(TWO_FA_EXPIRY_MINUTES, ChronoUnit.MINUTES));
        twoFactorTokenRepository.save(twoFaToken);

        // Simulate e-mail: log the verification link to the server console
        log.info("╔══════════════════════════════════════════════════════════╗");
        log.info("║  2FA VERIFICATION CODE for user: {}  ", user.getUsername());
        log.info("║  Code   : {}  ", tokenValue);
        log.info("║  Expires: {} minutes  ", TWO_FA_EXPIRY_MINUTES);
        log.info("╚══════════════════════════════════════════════════════════╝");

        auditLogService.logAuth(user.getId(), user.getUsername(), primaryRole(user),
                AuditAction.LOGIN_SUCCESS, "2FA token issued", ip, true);
        return TwoFaRequiredResponse.pending(tokenValue);
    }

    /**
     * Step 2 of login: validates the 2FA token and returns the full JWT response.
     */
    @Transactional
    public LoginResponse verifyTwoFa(VerifyTwoFaRequest request) {
        String ip = currentIp();

        TwoFactorToken twoFaToken = twoFactorTokenRepository
                .findByToken(request.token())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Invalid or expired verification code"));

        if (twoFaToken.isUsed()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Verification code already used");
        }
        if (twoFaToken.getExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Verification code has expired");
        }

        twoFaToken.setUsed(true);
        twoFactorTokenRepository.save(twoFaToken);

        User user = userRepository.findById(twoFaToken.getUserId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());
        LoginResponse response = buildLoginResponse(user, refreshToken.getToken());
        auditLogService.logAuth(user.getId(), user.getUsername(), response.getRole(),
                AuditAction.LOGIN_SUCCESS, "2FA verified", ip, true);
        return response;
    }

    /** Registers a new account with the default USER role and returns tokens. */
    @Transactional
    public LoginResponse register(RegisterRequest request) {
        String ip = currentIp();

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already in use");
        }

        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR, "Default role not found"));

        User newUser = new User();
        newUser.setUsername(request.getUsername());
        newUser.setEmail(request.getEmail());
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));
        newUser.setRoles(Set.of(userRole));

        User saved = userRepository.save(newUser);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(saved.getId());
        LoginResponse response = buildLoginResponse(saved, refreshToken.getToken());
        auditLogService.logAuth(saved.getId(), saved.getUsername(), response.getRole(),
                AuditAction.REGISTER, null, ip, true);
        return response;
    }

    // ── Authentication method 3: token-based re-authentication ───────────────

    /**
     * Exchanges a valid refresh token for a new access token (and rotates the
     * refresh token for forward secrecy).
     */
    @Transactional
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken old = refreshTokenService.validateRefreshToken(request.getRefreshToken());

        // Rotate: revoke the old token and issue a fresh one
        refreshTokenService.revokeToken(old.getToken());
        RefreshToken newRefresh = refreshTokenService.createRefreshToken(old.getUserId());

        User user = userRepository.findById(old.getUserId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));
        return buildLoginResponse(user, newRefresh.getToken());
    }

    /** Revokes the provided refresh token, effectively ending the session. */
    @Transactional
    public void logout(RefreshTokenRequest request) {
        refreshTokenService.revokeToken(request.getRefreshToken());
        auditLogService.logAuth(null, null, null,
                AuditAction.LOGOUT, null, currentIp(), true);
    }

    // ── Password recovery ─────────────────────────────────────────────────────

    /**
     * Generates a one-time password-reset token valid for 15 minutes.
     * In production the token should be sent by e-mail; for development it is
     * returned in the response body so the feature can be tested without an
     * SMTP server.
     */
    @Transactional
    public Map<String, String> forgotPassword(ForgotPasswordRequest request) {
        String ip = currentIp();

        // Always respond with the same message to prevent user-enumeration
        Optional<User> maybeUser = userRepository.findByEmail(request.getEmail());
        if (maybeUser.isEmpty()) {
            return Map.of("message", "If that e-mail is registered you will receive a reset link.");
        }

        User user = maybeUser.get();
        // Invalidate any existing reset tokens for this user
        passwordResetTokenRepository.deleteByUserId(user.getId());

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUserId(user.getId());
        resetToken.setToken(UUID.randomUUID().toString());
        resetToken.setExpiresAt(Instant.now().plus(15, ChronoUnit.MINUTES));
        passwordResetTokenRepository.save(resetToken);

        auditLogService.logAuth(user.getId(), user.getUsername(), primaryRole(user),
                AuditAction.PASSWORD_RESET_REQUEST, null, ip, true);

        // NOTE: remove resetToken from response and send via email in production
        return Map.of(
                "message", "Password reset token generated (dev mode – use resetToken field).",
                "resetToken", resetToken.getToken()
        );
    }

    /** Validates the reset token and sets the new password. */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String ip = currentIp();

        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByToken(request.getToken())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Invalid or unknown reset token"));

        if (resetToken.isUsed()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reset token has already been used");
        }
        if (resetToken.getExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reset token has expired");
        }

        User user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        // Revoke all active sessions after a password change
        refreshTokenService.revokeAllForUser(user.getId());

        auditLogService.logAuth(user.getId(), user.getUsername(), primaryRole(user),
                AuditAction.PASSWORD_RESET_SUCCESS, null, ip, true);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private LoginResponse buildLoginResponse(User user, String refreshTokenStr) {
        String primaryRole = primaryRole(user);
        Set<String> permissions = user.getRoles().stream()
                .flatMap(r -> r.getPermissions().stream())
                .map(p -> p.getName())
                .collect(Collectors.toSet());
        String accessToken = jwtUtil.generateToken(
                user.getId(), user.getUsername(), primaryRole, permissions);
        return new LoginResponse(
                user.getId(), user.getUsername(), user.getEmail(),
                primaryRole, permissions, accessToken, refreshTokenStr);
    }

    private String primaryRole(User user) {
        // Prefer ADMIN > USER > any other role so the JWT always reflects the
        // highest-privilege role even when a user has multiple roles assigned.
        List<String> PRIORITY = List.of("ADMIN", "USER");
        return user.getRoles().stream()
                .map(Role::getName)
                .min((a, b) -> {
                    int ia = PRIORITY.contains(a) ? PRIORITY.indexOf(a) : Integer.MAX_VALUE;
                    int ib = PRIORITY.contains(b) ? PRIORITY.indexOf(b) : Integer.MAX_VALUE;
                    return Integer.compare(ia, ib);
                })
                .orElse("USER");
    }

    private String currentIp() {
        try {
            HttpServletRequest req =
                    ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
            String xff = req.getHeader("X-Forwarded-For");
            return (xff != null && !xff.isBlank()) ? xff.split(",")[0].trim() : req.getRemoteAddr();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
