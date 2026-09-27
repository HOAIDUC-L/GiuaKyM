package com.course.common.protocol;

/**
 * Standardized error codes returned by the Server.
 */
public enum ErrorCode {
    INVALID_REQUEST("Yêu cầu không hợp lệ"),
    AUTH_FAILED("Tên đăng nhập hoặc mật khẩu không chính xác"),
    ACCOUNT_DISABLED("Tài khoản đã bị vô hiệu hóa hoặc khóa bởi quản trị viên"),
    ALREADY_LOGGED_IN("Tài khoản hiện đang được đăng nhập ở một phiên khác"),
    ACCESS_DENIED("Bạn không có quyền thực hiện hành động này"),
    NOT_AUTHENTICATED("Chưa đăng nhập vào hệ thống"),
    COURSE_NOT_FOUND("Không tìm thấy lớp học phần tương ứng"),
    COURSE_FULL("Lớp học phần đã hết chỗ đăng ký"),
    ALREADY_ENROLLED("Bạn đã đăng ký lớp học phần này trước đó"),
    DUPLICATE_SUBJECT("Bạn đã đăng ký một lớp học phần khác cùng môn học này"),
    SCHEDULE_CONFLICT("Trùng lịch học với môn học đã đăng ký trong thời khóa biểu"),
    ENROLLMENT_NOT_FOUND("Không tìm thấy thông tin đăng ký học phần để hủy"),
    SERVER_ERROR("Lỗi xử lý nội bộ phía máy chủ"),
    DATABASE_ERROR("Lỗi cơ sở dữ liệu");

    private final String defaultMessage;

    ErrorCode(String defaultMessage) {
        this.defaultMessage = defaultMessage;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
