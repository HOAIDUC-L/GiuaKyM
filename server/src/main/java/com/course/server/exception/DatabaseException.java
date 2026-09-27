package com.course.server.exception;

import com.course.common.protocol.ErrorCode;

public class DatabaseException extends CourseRegistrationException {
    public DatabaseException(String message, Throwable cause) {
        super(ErrorCode.DATABASE_ERROR, message, cause);
    }
}
