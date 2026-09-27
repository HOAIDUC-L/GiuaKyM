package com.course.server.repository;

import com.course.common.dto.EnrollmentDetailDto;
import com.course.common.model.Course;
import com.course.server.database.DatabaseManager;
import com.course.server.exception.DatabaseException;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Repository for Enrollment persistence operations.
 * Participates in transaction boundaries.
 */
public class EnrollmentRepository {
    private static final Logger LOGGER = Logger.getLogger(EnrollmentRepository.class.getName());
    private final DatabaseManager dbManager;

    public EnrollmentRepository() {
        this.dbManager = DatabaseManager.getInstance();
    }

    public EnrollmentRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    /**
     * Retrieves all course enrollments for a specific student, joined with course metadata.
     */
    public List<EnrollmentDetailDto> findByStudentId(String studentId) {
        String sql = "SELECT e.enrollment_id, e.student_id, e.course_id, e.registered_at, " +
                     "c.subject_code, c.course_name, c.lecturer_name, c.day_of_week, " +
                     "c.start_period, c.end_period, c.room " +
                     "FROM enrollments e " +
                     "JOIN courses c ON e.course_id = c.course_id " +
                     "WHERE e.student_id = ? " +
                     "ORDER BY c.day_of_week ASC, c.start_period ASC";

        List<EnrollmentDetailDto> list = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("registered_at");
                    list.add(new EnrollmentDetailDto(
                            rs.getLong("enrollment_id"),
                            rs.getString("student_id"),
                            rs.getString("course_id"),
                            rs.getString("subject_code"),
                            rs.getString("course_name"),
                            rs.getString("lecturer_name"),
                            rs.getInt("day_of_week"),
                            rs.getInt("start_period"),
                            rs.getInt("end_period"),
                            rs.getString("room"),
                            ts != null ? ts.toLocalDateTime() : null
                    ));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching enrollments for student: " + studentId, e);
            throw new DatabaseException("Lỗi truy vấn danh sách học phần đã đăng ký", e);
        }
        return list;
    }

    /**
     * Checks if student is already enrolled in this exact course_id.
     */
    public boolean isEnrolled(Connection conn, String studentId, String courseId) throws SQLException {
        String sql = "SELECT 1 FROM enrollments WHERE student_id = ? AND course_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, studentId);
            ps.setString(2, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Checks if student is already enrolled in another course with the same subject_code.
     */
    public boolean hasEnrolledSameSubject(Connection conn, String studentId, String subjectCode) throws SQLException {
        String sql = "SELECT 1 FROM enrollments e " +
                     "JOIN courses c ON e.course_id = c.course_id " +
                     "WHERE e.student_id = ? AND c.subject_code = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, studentId);
            ps.setString(2, subjectCode);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Fetches all courses the student is currently enrolled in to perform schedule conflict checks.
     */
    public List<Course> getEnrolledCourses(Connection conn, String studentId) throws SQLException {
        String sql = "SELECT c.course_id, c.subject_code, c.course_name, c.lecturer_name, " +
                     "c.day_of_week, c.start_period, c.end_period, c.room, c.max_slots, c.remaining_slots, c.created_at " +
                     "FROM enrollments e " +
                     "JOIN courses c ON e.course_id = c.course_id " +
                     "WHERE e.student_id = ?";
        List<Course> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("created_at");
                    list.add(new Course(
                            rs.getString("course_id"),
                            rs.getString("subject_code"),
                            rs.getString("course_name"),
                            rs.getString("lecturer_name"),
                            rs.getInt("day_of_week"),
                            rs.getInt("start_period"),
                            rs.getInt("end_period"),
                            rs.getString("room"),
                            rs.getInt("max_slots"),
                            rs.getInt("remaining_slots"),
                            ts != null ? ts.toLocalDateTime() : null
                    ));
                }
            }
        }
        return list;
    }

    /**
     * Inserts a new enrollment record.
     */
    public boolean insert(Connection conn, String studentId, String courseId) throws SQLException {
        String sql = "INSERT INTO enrollments (student_id, course_id) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, studentId);
            ps.setString(2, courseId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Deletes an enrollment record.
     */
    public boolean delete(Connection conn, String studentId, String courseId) throws SQLException {
        String sql = "DELETE FROM enrollments WHERE student_id = ? AND course_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, studentId);
            ps.setString(2, courseId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Counts enrollments for a course.
     */
    public int countByCourseId(Connection conn, String courseId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM enrollments WHERE course_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }
}
