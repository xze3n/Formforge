package com.formforge.config;

import com.formforge.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * One-time, idempotent migrator that re-encodes any legacy plaintext passwords
 * left by the V4 Flyway seed migration.
 *
 * <p>Runs at every startup but is a no-op when all stored passwords are already
 * BCrypt-encoded (i.e. start with {@code $2a$}).  This ensures the seeded accounts
 * ({@code admin} / {@code user1}) can be used as soon as the application first
 * starts after the BCrypt-based security layer is deployed.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordMigrator implements ApplicationRunner {

    private final UserRepository  userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        userRepository.findAll().forEach(user -> {
            String stored = user.getPassword();
            if (stored != null && !stored.startsWith("$2a$") && !stored.startsWith("$2b$")) {
                log.info("Re-encoding plaintext password for user '{}'", user.getUsername());
                user.setPassword(passwordEncoder.encode(stored));
                userRepository.save(user);
            }
        });
    }
}
