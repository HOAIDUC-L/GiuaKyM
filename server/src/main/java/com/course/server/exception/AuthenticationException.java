package com.course.server.exception;

import com.course.common.protocol.ErrorCode;

public class AuthenticationException extends CourseRegistrationException {
    public AuthenticationException(String message) {
        super(ErrorCode.AUTH_FAILED, message);
    }

    public AuthenticationException(ErrorCode code, String message) {
        super(code, message);
    }
}
