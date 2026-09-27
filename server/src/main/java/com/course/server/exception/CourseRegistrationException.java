package com.course.server.exception;

import com.course.common.protocol.ErrorCode;

/**
 * Base domain exception with an associated ErrorCode.
 */
public class CourseRegistrationException extends RuntimeException {
    private final ErrorCode errorCode;

    public CourseRegistrationException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public CourseRegistrationException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
