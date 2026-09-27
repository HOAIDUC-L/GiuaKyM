package com.course.server.exception;

import com.course.common.protocol.ErrorCode;

public class AlreadyEnrolledException extends CourseRegistrationException {
    public AlreadyEnrolledException(String message) {
        super(ErrorCode.ALREADY_ENROLLED, message);
    }
}
