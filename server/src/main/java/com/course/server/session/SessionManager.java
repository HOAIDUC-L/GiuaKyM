package com.course.server.session;

import com.course.common.dto.ClientSessionDto;
import com.course.common.model.Role;
import com.course.common.protocol.Action;
import com.course.common.protocol.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Thread-safe manager for all active client sessions.
 */
public class SessionManager {
    private static final Logger LOGGER = Logger.getLogger(SessionManager.class.getName());
    private static final SessionManager INSTANCE = new SessionManager();

    // Map of sessionId -> ClientSession
    private final Map<String, ClientSession> sessions = new ConcurrentHashMap<>();

    // Map of studentId -> sessionId for single-session enforcement
    private final Map<String, String> studentToSessionMap = new ConcurrentHashMap<>();

    // Disconnect listener callback
    private Consumer<ClientSession> onDisconnectListener;

    private SessionManager() {
    }

    public static SessionManager getInstance() {
        return INSTANCE;
    }

    public void setOnDisconnectListener(Consumer<ClientSession> listener) {
        this.onDisconnectListener = listener;
    }

    /**
     * Registers a newly connected client session (before authentication).
     */
    public void addSession(ClientSession session) {
        if (session != null) {
            sessions.put(session.getSessionId(), session);
            LOGGER.info(String.format("Session connected: %s from %s:%d (Total: %d)",
                    session.getSessionId(), session.getClientIp(), session.getClientPort(), sessions.size()));
        }
    }

    /**
     * Binds an authenticated student to their session.
     * Enforces single-session policy.
     *
     * @return true if successfully bound, false if account is already logged in.
     */
    public synchronized boolean bindUser(String studentId, String username, String email, String fullName, Role role, String sessionId) {
        if (studentId == null || sessionId == null) {
            return false;
        }

        // Single-session check: if student already has an active session
        String existingSessionId = studentToSessionMap.get(studentId);
        if (existingSessionId != null && !existingSessionId.equals(sessionId)) {
            ClientSession existingSession = sessions.get(existingSessionId);
            if (existingSession != null && !existingSession.getSocket().isClosed()) {
                LOGGER.warning("Single-session violation: " + studentId + " is already logged in on session " + existingSessionId);
                return false;
            } else {
                // Stale entry, cleanup
                studentToSessionMap.remove(studentId);
            }
        }

        ClientSession currentSession = sessions.get(sessionId);
        if (currentSession != null) {
            currentSession.setStudentId(studentId);
            currentSession.setUsername(username);
            currentSession.setEmail(email);
            currentSession.setFullName(fullName);
            currentSession.setRole(role);
            studentToSessionMap.put(studentId, sessionId);
            LOGGER.info(String.format("Session %s authenticated as %s (%s, %s)", sessionId, studentId, username, role));
            return true;
        }
        return false;
    }

    /**
     * Unbinds user on explicit LOGOUT without closing socket.
     */
    public synchronized void unbindUser(String sessionId) {
        ClientSession session = sessions.get(sessionId);
        if (session != null && session.getStudentId() != null) {
            studentToSessionMap.remove(session.getStudentId());
            LOGGER.info(String.format("Session %s logged out student %s", sessionId, session.getStudentId()));
            session.setStudentId(null);
            session.setUsername(null);
            session.setRole(null);
        }
    }

    /**
     * Removes session upon disconnect or shutdown.
     */
    public void removeSession(String sessionId) {
        if (sessionId == null) {
            return;
        }
        ClientSession session = sessions.remove(sessionId);
        if (session != null) {
            if (session.getStudentId() != null) {
                studentToSessionMap.remove(session.getStudentId(), sessionId);
            }
            if (onDisconnectListener != null) {
                try {
                    onDisconnectListener.accept(session);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error in disconnect listener callback", e);
                }
            }
            LOGGER.info(String.format("Session disconnected: %s (Total active: %d)", sessionId, sessions.size()));
        }
    }

    public ClientSession getSession(String sessionId) {
        return sessionId != null ? sessions.get(sessionId) : null;
    }

    public ClientSession getSessionByStudentId(String studentId) {
        if (studentId == null) {
            return null;
        }
        String sessionId = studentToSessionMap.get(studentId);
        return sessionId != null ? sessions.get(sessionId) : null;
    }

    public boolean isStudentOnline(String studentId) {
        if (studentId == null) {
            return false;
        }
        return studentToSessionMap.containsKey(studentId);
    }

    public List<ClientSession> getAllSessions() {
        return new ArrayList<>(sessions.values());
    }

    public List<ClientSessionDto> getAllSessionDtos() {
        List<ClientSessionDto> list = new ArrayList<>();
        for (ClientSession session : sessions.values()) {
            list.add(session.toDto());
        }
        return list;
    }

    public int getOnlineCount() {
        return sessions.size();
    }

    /**
     * Kicks a student by studentId or sessionId with FORCE_LOGOUT notification.
     */
    public boolean kickSession(String identifier, String reason) {
        ClientSession target = null;
        if (sessions.containsKey(identifier)) {
            target = sessions.get(identifier);
        } else if (studentToSessionMap.containsKey(identifier)) {
            target = sessions.get(studentToSessionMap.get(identifier));
        }

        if (target != null) {
            String message = (reason != null && !reason.isBlank()) ? reason : "Bạn đã bị quản trị viên ngắt kết nối khỏi hệ thống.";
            LOGGER.info("Admin kicking session " + target.getSessionId() + " (User: " + target.getStudentId() + ")");
            target.sendResponse(Response.forceLogout(message));
            target.close();
            removeSession(target.getSessionId());
            return true;
        }
        return false;
    }

    /**
     * Broadcasts a response to all online clients.
     */
    public void broadcast(Response response) {
        if (response == null) {
            return;
        }
        for (ClientSession session : sessions.values()) {
            session.sendResponse(response);
        }
    }

    /**
     * Broadcasts a response to all online clients except the specified sessionId.
     */
    public void broadcastExcept(String exceptSessionId, Response response) {
        if (response == null) {
            return;
        }
        for (ClientSession session : sessions.values()) {
            if (!session.getSessionId().equals(exceptSessionId)) {
                session.sendResponse(response);
            }
        }
    }

    /**
     * Broadcasts a response only to clients with a specific role.
     */
    public void broadcastToRole(Role role, Response response) {
        if (response == null || role == null) {
            return;
        }
        for (ClientSession session : sessions.values()) {
            if (role.equals(session.getRole())) {
                session.sendResponse(response);
            }
        }
    }

    /**
     * Disconnects all clients gracefully (e.g. during server shutdown).
     */
    public void closeAllSessions() {
        LOGGER.info("Closing all client sessions for server shutdown...");
        Response shutdownMsg = Response.forceLogout("Máy chủ đang dừng hoạt động để bảo trì.");
        for (ClientSession session : sessions.values()) {
            session.sendResponse(shutdownMsg);
            session.close();
        }
        sessions.clear();
        studentToSessionMap.clear();
    }
}
