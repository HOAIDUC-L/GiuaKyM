package com.course.server.repository;

import com.course.server.database.DatabaseManager;
import com.course.server.exception.DatabaseException;

import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Repository for session audit logging.
 */
public class SessionLogRepository {
    private static final Logger LOGGER = Logger.getLogger(SessionLogRepository.class.getName());
    private final DatabaseManager dbManager;

    public SessionLogRepository() {
        this.dbManager = DatabaseManager.getInstance();
    }

    public SessionLogRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public Long createLog(String studentId, String clientIp, int clientPort) {
        String sql = "INSERT INTO session_logs (student_id, client_ip, client_port, login_time, status) " +
                     "VALUES (?, ?, ?, CURRENT_TIMESTAMP, 'ONLINE')";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, studentId);
            ps.setString(2, clientIp);
            ps.setInt(3, clientPort);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error creating session log for student: " + studentId, e);
        }
        return null;
    }

    public void updateStatus(Long logId, String status) {
        if (logId == null) {
            return;
        }
        String sql = "UPDATE session_logs SET logout_time = CURRENT_TIMESTAMP, status = ? WHERE session_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setLong(2, logId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error updating session log status: " + logId, e);
        }
    }
}
