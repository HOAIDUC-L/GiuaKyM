package com.course.server.exception;

import com.course.common.protocol.ErrorCode;

public class DuplicateSubjectException extends CourseRegistrationException {
    public DuplicateSubjectException(String message) {
        super(ErrorCode.DUPLICATE_SUBJECT, message);
    }
}
