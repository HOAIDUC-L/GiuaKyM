package com.course.server.session;

import com.course.common.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.*;

class SingleSessionPolicyTest {

    private SessionManager sessionManager;

    @BeforeEach
    void setUp() {
        sessionManager = SessionManager.getInstance();
        sessionManager.closeAllSessions();
    }

    @Test
    @DisplayName("Single-session policy: 1 student = 1 active socket. Reject concurrent second login")
    void testSingleSessionEnforcement() {
        Socket mockSocket1 = new Socket();
        Socket mockSocket2 = new Socket();

        ClientSession session1 = new ClientSession("SESS-1", mockSocket1, new PrintWriter(new StringWriter()));
        ClientSession session2 = new ClientSession("SESS-2", mockSocket2, new PrintWriter(new StringWriter()));

        sessionManager.addSession(session1);
        sessionManager.addSession(session2);

        // First login succeeds
        boolean firstBound = sessionManager.bindUser("SV001", "sv01", "sv01@ptit.edu.vn", "Nguyen Van A", Role.STUDENT, "SESS-1");
        assertTrue(firstBound, "First login for SV001 must succeed");
        assertTrue(sessionManager.isStudentOnline("SV001"));

        // Second login from another socket/session fails
        boolean secondBound = sessionManager.bindUser("SV001", "sv01", "sv01@ptit.edu.vn", "Nguyen Van A", Role.STUDENT, "SESS-2");
        assertFalse(secondBound, "Second login attempt for SV001 must be rejected by single-session policy");

        // Logout first session
        sessionManager.unbindUser("SESS-1");
        assertFalse(sessionManager.isStudentOnline("SV001"));

        // Now second session can log in
        boolean retryBound = sessionManager.bindUser("SV001", "sv01", "sv01@ptit.edu.vn", "Nguyen Van A", Role.STUDENT, "SESS-2");
        assertTrue(retryBound, "After previous logout, second session can now log in");
    }
}
