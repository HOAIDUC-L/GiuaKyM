package com.course.server.repository;

import com.course.common.dto.SearchCourseRequestDto;
import com.course.common.model.Course;
import com.course.server.database.DatabaseManager;
import com.course.server.exception.DatabaseException;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Repository for Course persistence operations.
 * Implements row-level locking (SELECT ... FOR UPDATE) for concurrency control.
 */
public class CourseRepository {
    private static final Logger LOGGER = Logger.getLogger(CourseRepository.class.getName());
    private final DatabaseManager dbManager;

    public CourseRepository() {
        this.dbManager = DatabaseManager.getInstance();
    }

    public CourseRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public List<Course> findAll() {
        String sql = "SELECT course_id, subject_code, course_name, lecturer_name, " +
                     "day_of_week, start_period, end_period, room, max_slots, remaining_slots, created_at " +
                     "FROM courses ORDER BY day_of_week ASC, start_period ASC, course_id ASC";
        List<Course> list = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRowToCourse(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error listing all courses", e);
            throw new DatabaseException("Lỗi truy vấn danh sách học phần", e);
        }
        return list;
    }

    public List<Course> search(String keyword, SearchCourseRequestDto.SearchType searchType) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return findAll();
        }

        String pattern = "%" + keyword.trim().toLowerCase() + "%";
        StringBuilder sql = new StringBuilder("SELECT course_id, subject_code, course_name, lecturer_name, ")
                .append("day_of_week, start_period, end_period, room, max_slots, remaining_slots, created_at ")
                .append("FROM courses WHERE ");

        switch (searchType) {
            case COURSE_NAME:
                sql.append("LOWER(course_name) LIKE ? ");
                break;
            case SUBJECT_CODE:
                sql.append("LOWER(subject_code) LIKE ? ");
                break;
            case LECTURER:
                sql.append("LOWER(lecturer_name) LIKE ? ");
                break;
            case ALL:
            default:
                sql.append("(LOWER(course_name) LIKE ? OR LOWER(subject_code) LIKE ? OR LOWER(lecturer_name) LIKE ? OR LOWER(course_id) LIKE ?) ");
                break;
        }
        sql.append("ORDER BY day_of_week ASC, start_period ASC, course_id ASC");

        List<Course> list = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            if (searchType == SearchCourseRequestDto.SearchType.ALL) {
                ps.setString(1, pattern);
                ps.setString(2, pattern);
                ps.setString(3, pattern);
                ps.setString(4, pattern);
            } else {
                ps.setString(1, pattern);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToCourse(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error searching courses with keyword: " + keyword, e);
            throw new DatabaseException("Lỗi tìm kiếm học phần", e);
        }
        return list;
    }

    public Optional<Course> findById(String courseId) {
        String sql = "SELECT course_id, subject_code, course_name, lecturer_name, " +
                     "day_of_week, start_period, end_period, room, max_slots, remaining_slots, created_at " +
                     "FROM courses WHERE course_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToCourse(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding course by ID: " + courseId, e);
            throw new DatabaseException("Lỗi tìm kiếm học phần theo mã", e);
        }
        return Optional.empty();
    }

    /**
     * Finds and locks the course row using SELECT ... FOR UPDATE.
     * MUST be called within an active transaction with autocommit=false.
     */
    public Optional<Course> findByIdForUpdate(Connection conn, String courseId) throws SQLException {
        String sql = "SELECT course_id, subject_code, course_name, lecturer_name, " +
                     "day_of_week, start_period, end_period, room, max_slots, remaining_slots, created_at " +
                     "FROM courses WHERE course_id = ? FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToCourse(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Updates the remaining slots of a course.
     * MUST be called within the same transaction that holds the lock.
     */
    public boolean updateRemainingSlots(Connection conn, String courseId, int newRemainingSlots) throws SQLException {
        String sql = "UPDATE courses SET remaining_slots = ? WHERE course_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, newRemainingSlots);
            ps.setString(2, courseId);
            return ps.executeUpdate() > 0;
        }
    }

    private Course mapRowToCourse(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("created_at");
        return new Course(
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
        );
    }
}
