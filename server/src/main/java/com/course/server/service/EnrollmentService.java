package com.course.server.service;

import com.course.common.constants.AppConstants;
import com.course.common.dto.CancelCourseRequestDto;
import com.course.common.dto.CourseUpdatedPushDto;
import com.course.common.dto.EnrollmentDetailDto;
import com.course.common.dto.RegisterCourseRequestDto;
import com.course.common.model.Course;
import com.course.common.model.User;
import com.course.common.protocol.Action;
import com.course.common.protocol.ErrorCode;
import com.course.common.protocol.Response;
import com.course.server.database.DatabaseManager;
import com.course.server.exception.*;
import com.course.server.network.ProtocolHandler;
import com.course.server.repository.CourseRepository;
import com.course.server.repository.EnrollmentRepository;
import com.course.server.repository.UserRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service orchestrating course registrations and cancellations.
 * Strictly guarantees ACID transaction isolation and prevents race conditions
 * using row-level locking (SELECT ... FOR UPDATE).
 */
public class EnrollmentService implements ProtocolHandler.EnrollmentDelegate {
    private static final Logger LOGGER = Logger.getLogger(EnrollmentService.class.getName());

    private final DatabaseManager dbManager;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final BroadcastService broadcastService;

    public EnrollmentService() {
        this.dbManager = DatabaseManager.getInstance();
        this.courseRepository = new CourseRepository();
        this.enrollmentRepository = new EnrollmentRepository();
        this.userRepository = new UserRepository();
        this.broadcastService = BroadcastService.getInstance();
    }

    public EnrollmentService(DatabaseManager dbManager, CourseRepository courseRepository,
                             EnrollmentRepository enrollmentRepository, UserRepository userRepository,
                             BroadcastService broadcastService) {
        this.dbManager = dbManager;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
        this.broadcastService = broadcastService;
    }

    @Override
    public Response getMyEnrollments(String studentId, String requestId) {
        try {
            List<EnrollmentDetailDto> list = enrollmentRepository.findByStudentId(studentId);
            return Response.success(Action.GET_MY_ENROLLMENTS_RESPONSE, requestId, "Lấy danh sách học phần đã đăng ký thành công.", list);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to get enrollments for student " + studentId, e);
            return Response.error(Action.GET_MY_ENROLLMENTS_RESPONSE, requestId, ErrorCode.DATABASE_ERROR, "Lỗi truy vấn danh sách học phần đã đăng ký.");
        }
    }

    @Override
    public Response registerCourse(String studentId, RegisterCourseRequestDto req, String requestId) {
        String courseId = req.getCourseId();
        Connection conn = null;

        try {
            conn = dbManager.getConnection();
            conn.setAutoCommit(false); // BEGIN TRANSACTION

            // 1. SELECT course FOR UPDATE (Row-level lock to prevent concurrent slot deduction)
            Optional<Course> courseOpt = courseRepository.findByIdForUpdate(conn, courseId);
            if (courseOpt.isEmpty()) {
                throw new CourseNotFoundException("Lớp học phần không tồn tại: " + courseId);
            }
            Course course = courseOpt.get();

            // 2. Check if student account is still active
            Optional<User> userOpt = userRepository.findByStudentId(studentId);
            if (userOpt.isEmpty() || !userOpt.get().isActive()) {
                throw new AuthenticationException(ErrorCode.ACCOUNT_DISABLED, "Tài khoản của bạn đã bị khóa hoặc không tồn tại.");
            }

            // 3. Check if student already enrolled in this exact course
            if (enrollmentRepository.isEnrolled(conn, studentId, courseId)) {
                throw new AlreadyEnrolledException("Bạn đã đăng ký lớp học phần này trước đó.");
            }

            // 4. Check if student already enrolled in another course with the SAME subject_code
            if (enrollmentRepository.hasEnrolledSameSubject(conn, studentId, course.getSubjectCode())) {
                throw new DuplicateSubjectException("Bạn đã đăng ký một lớp học phần khác thuộc môn học (" + course.getSubjectCode() + ").");
            }

            // 5. Check schedule conflict against all current enrolled courses
            List<Course> enrolledCourses = enrollmentRepository.getEnrolledCourses(conn, studentId);
            for (Course existingCourse : enrolledCourses) {
                if (course.conflictsWith(existingCourse)) {
                    String conflictDetails = String.format(
                            "Trùng lịch với môn '%s' (%s) vào %s, tiết %d-%d.",
                            existingCourse.getCourseName(),
                            existingCourse.getCourseId(),
                            AppConstants.getDayName(existingCourse.getDayOfWeek()),
                            existingCourse.getStartPeriod(),
                            existingCourse.getEndPeriod()
                    );
                    throw new ScheduleConflictException(conflictDetails);
                }
            }

            // 6. Check remaining slots > 0
            if (course.getRemainingSlots() <= 0) {
                throw new CourseFullException("Lớp học phần " + courseId + " đã hết chỗ đăng ký.");
            }

            // 7. Deduct remaining slot
            int updatedSlots = course.getRemainingSlots() - 1;
            boolean slotUpdated = courseRepository.updateRemainingSlots(conn, courseId, updatedSlots);
            if (!slotUpdated) {
                throw new DatabaseException("Không thể cập nhật số lượng slot còn lại.", null);
            }

            // 8. Insert into enrollments
            boolean enrolled = enrollmentRepository.insert(conn, studentId, courseId);
            if (!enrolled) {
                throw new DatabaseException("Không thể lưu thông tin đăng ký học phần.", null);
            }

            // 9. COMMIT TRANSACTION
            conn.commit();
            LOGGER.info(String.format("Student %s registered successfully to %s. Remaining slots: %d", studentId, courseId, updatedSlots));

            // 10. Broadcast slot update in real-time to ALL online clients
            broadcastService.notifyCourseUpdated(courseId, updatedSlots);

            CourseUpdatedPushDto resultData = new CourseUpdatedPushDto(courseId, updatedSlots);
            return Response.success(Action.REGISTER_COURSE_RESPONSE, requestId, "Đăng ký học phần thành công.", resultData);

        } catch (CourseRegistrationException e) {
            rollbackQuietly(conn);
            LOGGER.info(String.format("Registration rejected for student %s on course %s: [%s] %s",
                    studentId, courseId, e.getErrorCode(), e.getMessage()));
            return Response.error(Action.REGISTER_COURSE_RESPONSE, requestId, e.getErrorCode(), e.getMessage());

        } catch (SQLException e) {
            rollbackQuietly(conn);
            LOGGER.log(Level.SEVERE, "SQL error during registration transaction for student " + studentId, e);
            return Response.error(Action.REGISTER_COURSE_RESPONSE, requestId, ErrorCode.DATABASE_ERROR, "Lỗi cơ sở dữ liệu khi đăng ký học phần.");

        } finally {
            closeQuietly(conn);
        }
    }

    @Override
    public Response cancelCourse(String studentId, CancelCourseRequestDto req, String requestId) {
        String courseId = req.getCourseId();
        Connection conn = null;

        try {
            conn = dbManager.getConnection();
            conn.setAutoCommit(false); // BEGIN TRANSACTION

            // 1. SELECT course FOR UPDATE
            Optional<Course> courseOpt = courseRepository.findByIdForUpdate(conn, courseId);
            if (courseOpt.isEmpty()) {
                throw new CourseNotFoundException("Lớp học phần không tồn tại: " + courseId);
            }
            Course course = courseOpt.get();

            // 2. Check if student is actually enrolled
            if (!enrollmentRepository.isEnrolled(conn, studentId, courseId)) {
                throw new EnrollmentNotFoundException("Bạn chưa đăng ký lớp học phần này.");
            }

            // 3. Delete enrollment record
            boolean deleted = enrollmentRepository.delete(conn, studentId, courseId);
            if (!deleted) {
                throw new DatabaseException("Không thể xóa thông tin đăng ký học phần.", null);
            }

            // 4. Increment remaining slots (must not exceed max_slots)
            int updatedSlots = Math.min(course.getMaxSlots(), course.getRemainingSlots() + 1);
            boolean slotUpdated = courseRepository.updateRemainingSlots(conn, courseId, updatedSlots);
            if (!slotUpdated) {
                throw new DatabaseException("Không thể cập nhật số lượng slot học phần.", null);
            }

            // 5. COMMIT TRANSACTION
            conn.commit();
            LOGGER.info(String.format("Student %s cancelled enrollment in %s. Remaining slots: %d", studentId, courseId, updatedSlots));

            // 6. Broadcast slot update in real-time to ALL online clients
            broadcastService.notifyCourseUpdated(courseId, updatedSlots);

            CourseUpdatedPushDto resultData = new CourseUpdatedPushDto(courseId, updatedSlots);
            return Response.success(Action.CANCEL_COURSE_RESPONSE, requestId, "Hủy đăng ký học phần thành công.", resultData);

        } catch (CourseRegistrationException e) {
            rollbackQuietly(conn);
            LOGGER.info(String.format("Cancellation rejected for student %s on course %s: [%s] %s",
                    studentId, courseId, e.getErrorCode(), e.getMessage()));
            return Response.error(Action.CANCEL_COURSE_RESPONSE, requestId, e.getErrorCode(), e.getMessage());

        } catch (SQLException e) {
            rollbackQuietly(conn);
            LOGGER.log(Level.SEVERE, "SQL error during cancellation transaction for student " + studentId, e);
            return Response.error(Action.CANCEL_COURSE_RESPONSE, requestId, ErrorCode.DATABASE_ERROR, "Lỗi cơ sở dữ liệu khi hủy đăng ký học phần.");

        } finally {
            closeQuietly(conn);
        }
    }

    private void rollbackQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException ex) {
                LOGGER.log(Level.WARNING, "Failed to rollback transaction", ex);
            }
        }
    }

    private void closeQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.setAutoCommit(true);
                conn.close();
            } catch (SQLException ex) {
                LOGGER.log(Level.WARNING, "Failed to close database connection", ex);
            }
        }
    }
}
