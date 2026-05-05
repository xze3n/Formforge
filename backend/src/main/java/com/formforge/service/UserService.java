package com.formforge.service;

import com.formforge.dto.LoginRequest;
import com.formforge.dto.LoginResponse;
import com.formforge.dto.RegisterRequest;
import com.formforge.model.AuditAction;
import com.formforge.model.Role;
import com.formforge.model.User;
import com.formforge.repository.RoleRepository;
import com.formforge.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuditLogService auditLogService;

    /** Validates credentials and returns the user's profile + permissions. */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String ip = currentIp();
        Optional<User> maybeUser = userRepository.findByEmail(request.getEmail());

        if (maybeUser.isEmpty()) {
            auditLogService.logAuth(null, request.getEmail(), null,
                    AuditAction.LOGIN_FAILURE, "Unknown email", ip, false);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        User user = maybeUser.get();
        if (!user.getPassword().equals(request.getPassword())) {
            auditLogService.logAuth(user.getId(), user.getUsername(), primaryRole(user),
                    AuditAction.LOGIN_FAILURE, "Wrong password", ip, false);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        LoginResponse response = toResponse(user);
        auditLogService.logAuth(user.getId(), user.getUsername(), response.getRole(),
                AuditAction.LOGIN_SUCCESS, null, ip, true);
        return response;
    }

    /** Registers a new account with the default USER role. */
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
        newUser.setPassword(request.getPassword());
        newUser.setRoles(Set.of(userRole));

        User saved = userRepository.save(newUser);
        LoginResponse response = toResponse(saved);
        auditLogService.logAuth(saved.getId(), saved.getUsername(), response.getRole(),
                AuditAction.REGISTER, null, ip, true);
        return response;
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private LoginResponse toResponse(User user) {
        String primaryRole = primaryRole(user);
        Set<String> permissions = user.getRoles().stream()
                .flatMap(r -> r.getPermissions().stream())
                .map(p -> p.getName())
                .collect(Collectors.toSet());
        return new LoginResponse(user.getId(), user.getUsername(), user.getEmail(),
                primaryRole, permissions);
    }

    private String primaryRole(User user) {
        return user.getRoles().stream().map(Role::getName).findFirst().orElse("USER");
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
