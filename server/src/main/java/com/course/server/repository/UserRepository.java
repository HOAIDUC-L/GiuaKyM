package com.course.server.repository;

import com.course.common.model.Role;
import com.course.common.model.User;
import com.course.server.database.DatabaseManager;
import com.course.server.exception.DatabaseException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Repository for User persistence operations using JDBC PreparedStatement.
 */
public class UserRepository {
    private static final Logger LOGGER = Logger.getLogger(UserRepository.class.getName());
    private final DatabaseManager dbManager;

    public UserRepository() {
        this.dbManager = DatabaseManager.getInstance();
    }

    public UserRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public Optional<User> findByUsername(String username) {
        String sql = "SELECT student_id, email, username, password, full_name, class_name, role, is_active, created_at " +
                     "FROM users WHERE username = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToUser(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding user by username: " + username, e);
            throw new DatabaseException("Lỗi truy vấn người dùng theo tên đăng nhập", e);
        }
        return Optional.empty();
    }

    public Optional<User> findByStudentId(String studentId) {
        String sql = "SELECT student_id, email, username, password, full_name, class_name, role, is_active, created_at " +
                     "FROM users WHERE student_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToUser(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding user by studentId: " + studentId, e);
            throw new DatabaseException("Lỗi truy vấn người dùng theo MSSV", e);
        }
        return Optional.empty();
    }

    public Optional<User> findByIdentifier(String identifier) {
        String sql = "SELECT student_id, email, username, password, full_name, class_name, role, is_active, created_at " +
                     "FROM users WHERE student_id = ? OR username = ? OR email = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, identifier);
            ps.setString(2, identifier);
            ps.setString(3, identifier);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToUser(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding user by identifier: " + identifier, e);
            throw new DatabaseException("Lỗi tìm kiếm người dùng theo định danh", e);
        }
        return Optional.empty();
    }

    public boolean updateIsActive(String studentId, boolean isActive) {
        String sql = "UPDATE users SET is_active = ? WHERE student_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, isActive);
            ps.setString(2, studentId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating user active status: " + studentId, e);
            throw new DatabaseException("Lỗi cập nhật trạng thái tài khoản", e);
        }
    }

    private User mapRowToUser(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("created_at");
        return new User(
                rs.getString("student_id"),
                rs.getString("email"),
                rs.getString("username"),
                rs.getString("password"),
                rs.getString("full_name"),
                rs.getString("class_name"),
                Role.fromString(rs.getString("role")),
                rs.getBoolean("is_active"),
                ts != null ? ts.toLocalDateTime() : null
        );
    }
}
