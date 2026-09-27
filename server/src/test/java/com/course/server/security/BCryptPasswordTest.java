package com.course.server.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;

import static org.junit.jupiter.api.Assertions.*;

class BCryptPasswordTest {

    // Hashes seeded in database/seed.sql
    private static final String STUDENT_HASH = "$2a$12$7Bu8DxKorw8zQ.xP0QvmF.OGfRrybZw.UADjJgwbWNV0ej.pKyz26";
    private static final String ADMIN_HASH = "$2a$12$QAPC1dxlMLegcEOhi0V4E.Kurj80xZOKsmSDtxu0grsDBW8eEn5pa";

    @Test
    @DisplayName("Verify correct passwords for seed accounts against BCrypt hashes")
    void testPasswordVerification() {
        assertTrue(BCrypt.checkpw("123456", STUDENT_HASH), "Password '123456' must match student hash");
        assertTrue(BCrypt.checkpw("admin123", ADMIN_HASH), "Password 'admin123' must match admin hash");
    }

    @Test
    @DisplayName("Reject wrong passwords")
    void testWrongPasswordRejected() {
        assertFalse(BCrypt.checkpw("wrongpassword", STUDENT_HASH));
        assertFalse(BCrypt.checkpw("1234567", STUDENT_HASH));
        assertFalse(BCrypt.checkpw("admin1234", ADMIN_HASH));
    }
}
