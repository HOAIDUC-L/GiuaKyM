package com.course.server.exception;

import com.course.common.protocol.ErrorCode;

public class EnrollmentNotFoundException extends CourseRegistrationException {
    public EnrollmentNotFoundException(String message) {
        super(ErrorCode.ENROLLMENT_NOT_FOUND, message);
    }
}
