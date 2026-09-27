package com.course.server.exception;

import com.course.common.protocol.ErrorCode;

public class CourseFullException extends CourseRegistrationException {
    public CourseFullException(String message) {
        super(ErrorCode.COURSE_FULL, message);
    }
}
