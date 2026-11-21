package com.skr.erp.security;

import com.skr.erp.exception.BusinessException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/** One place for password rules and BCrypt hashing (create user, reset, change). */
@Service
public class PasswordService {

    private static final int MIN_LENGTH = 8;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    // Validates a new password and returns its BCrypt hash.
    public String hashNew(String rawPassword) {
        if (rawPassword == null || rawPassword.length() < MIN_LENGTH) {
            throw new BusinessException("Password must be at least " + MIN_LENGTH + " characters");
        }
        return encoder.encode(rawPassword);
    }

    public boolean matches(String rawPassword, String hash) {
        return rawPassword != null && hash != null && hash.startsWith("$2") && encoder.matches(rawPassword, hash);
    }
}
