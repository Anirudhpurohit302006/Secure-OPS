package com.secureops.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** Prints a BCrypt hash for ADMIN_PASSWORD_HASH. Used by scripts/hash-password.ps1 (password via env var, not argv). */
public final class PasswordHashTool {
    private PasswordHashTool() {}

    public static void main(String[] args) {
        String pw = System.getenv("SECUREOPS_NEW_PASSWORD");
        if (pw == null || pw.length() < 8) {
            System.err.println("Set SECUREOPS_NEW_PASSWORD (min 8 chars).");
            System.exit(1);
        }
        System.out.println(new BCryptPasswordEncoder().encode(pw));
    }
}
