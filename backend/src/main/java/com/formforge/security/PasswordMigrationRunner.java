package com.formforge.security;

import com.formforge.model.User;
import com.formforge.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs once at startup to ensure all stored passwords are BCrypt-encoded.
 *
 * <p>The Flyway seed scripts (V4) insert plain-text passwords for convenience.
 * This runner detects any passwords that are not yet BCrypt hashes (they do not
 * start with {@code $2a$} or {@code $2b$}) and re-encodes them in-place, so the
 * application works correctly from the first start without requiring a separate
 * migration step that would need hard-coded BCrypt hashes.
 *
 * <p>On subsequent startups all passwords already begin with the BCrypt prefix
 * and are skipped – the operation is idempotent.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordMigrationRunner implements ApplicationRunner {

    private final UserRepository  userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int migrated = 0;
        for (User user : userRepository.findAll()) {
            String pwd = user.getPassword();
            if (!isBCrypt(pwd)) {
                user.setPassword(passwordEncoder.encode(pwd));
                userRepository.save(user);
                migrated++;
                log.info("Password migrated to BCrypt for user '{}'", user.getUsername());
            }
        }
        if (migrated > 0) {
            log.info("Password migration complete: {} account(s) updated.", migrated);
        }
    }

    private boolean isBCrypt(String password) {
        return password != null
                && (password.startsWith("$2a$") || password.startsWith("$2b$"));
    }
}
